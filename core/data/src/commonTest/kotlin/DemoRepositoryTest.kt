import com.whatever.caro.core.data.di.dataModule
import com.whatever.caro.core.data.mapper.toUser
import com.whatever.caro.core.data.repository.demo.DemoRepository
import com.whatever.caro.core.remote.datasource.demo.DemoDataSource
import com.whatever.caro.core.remote.di.networkModule
import com.whatever.caro.core.remote.di.remoteModule
import com.whatever.caro.core.remote.model.demo.DemoDto
import com.whatever.caro.core.remote.model.demo.response.DemoResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
class DemoRepositoryTest :
    FunSpec(),
    KoinTest {
    private lateinit var overrideModule: Module

    init {
        extensions(
            KoinExtension(
                listOf(
                    dataModule,
                    remoteModule,
                    networkModule,
                ),
            ),
        )

        test("getData - datasource 호출 + 매핑") {
            runTest {
                val demoDataSourceMock = mock<DemoDataSource>()

                overrideModule =
                    module {
                        single<DemoDataSource> { demoDataSourceMock }
                    }

                loadKoinModules(overrideModule)

                everySuspend {
                    demoDataSourceMock.getRestWithQuery(query = 123)
                } returns
                    DemoResponse(
                        user = DemoDto(id = 123L, name = "테스터"),
                    )

                val repo: DemoRepository = get()

                repo.getData(123L) shouldBe
                    DemoResponse(
                        user = DemoDto(id = 123L, name = "테스터"),
                    ).toUser()
            }
        }

        afterTest {
            if (::overrideModule.isInitialized) {
                unloadKoinModules(overrideModule)
            }
        }
    }
}
