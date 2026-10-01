import app.cash.turbine.test
import com.whatever.caro.core.data.repository.library.LibraryRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.model.library.LibraryDetail
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.library.LibraryPreviewViewModel
import com.whatever.caro.feature.home.library.LibraryViewModel
import com.whatever.caro.feature.home.library.mvi.LibraryFailure
import com.whatever.caro.feature.home.library.mvi.LibraryIntent
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent
import com.whatever.caro.feature.home.library.mvi.LibrarySideEffect
import com.whatever.caro.feature.home.library.mvi.toUiContent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest :
    FunSpec({
        val dispatcher = StandardTestDispatcher()
        beforeTest { Dispatchers.setMain(dispatcher) }
        afterTest { Dispatchers.resetMain() }

        test("중복 탭은 하나의 요청이며 성공 후 상세 이동, 새 추가는 새 key를 쓴다") {
            runTest(dispatcher) {
                val gate = CompletableDeferred<Unit>()
                val repository = FakeLibraryRepository()
                repository.copyAction = {
                    gate.await()
                    repository.personal
                }
                val vm = LibraryPreviewViewModel(1, repository, ExceptionFilter.None)
                vm.intent(LibraryPreviewIntent.Initialize)
                advanceUntilIdle()
                vm.sideEffect.test {
                    vm.intent(LibraryPreviewIntent.Add)
                    vm.intent(LibraryPreviewIntent.Add)
                    runCurrent()
                    repository.keys.size shouldBe 1
                    vm.state.value.adding shouldBe true
                    gate.complete(Unit)
                    advanceUntilIdle()
                    awaitItem() shouldBe LibrarySideEffect.PersonalDeck(repository.personal)
                    vm.intent(LibraryPreviewIntent.Add)
                    advanceUntilIdle()
                    awaitItem() shouldBe LibrarySideEffect.PersonalDeck(repository.personal)
                    repository.keys.distinct().size shouldBe 2
                }
            }
        }
        test("추가 오류와 확인은 미리보기를 유지하고 재요청은 사용자가 눌러야 한다") {
            runTest(dispatcher) {
                val repository = FakeLibraryRepository()
                repository.copyAction = { error("network response lost") }
                val vm = LibraryPreviewViewModel(1, repository, ExceptionFilter.None)
                vm.intent(LibraryPreviewIntent.Initialize)
                advanceUntilIdle()
                vm.sideEffect.test {
                    vm.intent(LibraryPreviewIntent.Add)
                    advanceUntilIdle()
                    vm.state.value.showAddError shouldBe true
                    vm.state.value.detail shouldBe repository.preview.toUiContent()
                    vm.intent(LibraryPreviewIntent.DismissError)
                    advanceUntilIdle()
                    vm.state.value.showAddError shouldBe false
                    repository.keys.size shouldBe 1
                    expectNoEvents()
                    repository.copyAction = { repository.personal }
                    vm.intent(LibraryPreviewIntent.Add)
                    advanceUntilIdle()
                    repository.keys.size shouldBe 2
                    repository.keys.distinct().size shouldBe 1
                    awaitItem() shouldBe LibrarySideEffect.PersonalDeck(repository.personal)
                }
            }
        }
        test("게시 종료는 미리보기를 표시하지 않고 권한 오류를 구분한다") {
            runTest(dispatcher) {
                val repository = FakeLibraryRepository()
                repository.detailAction = { throw CaroServerException("L001", "gone", "gone") }
                val vm = LibraryPreviewViewModel(1, repository, ExceptionFilter.None)
                vm.intent(LibraryPreviewIntent.Initialize)
                advanceUntilIdle()
                vm.state.value.failure shouldBe LibraryFailure.UNAVAILABLE
                vm.intent(LibraryPreviewIntent.Add)
                advanceUntilIdle()
                repository.keys shouldBe emptyList()
                repository.detailAction = { throw CaroServerException("A002", "forbidden", "forbidden") }
                vm.intent(LibraryPreviewIntent.Retry)
                advanceUntilIdle()
                vm.state.value.failure shouldBe LibraryFailure.FORBIDDEN
            }
        }
        test("새로고침 실패 시 이전 목록을 유지하고 오래된 요청이 최신 응답을 덮지 않는다") {
            runTest(dispatcher) {
                val repository = FakeLibraryRepository()
                val vm = LibraryViewModel(repository, ExceptionFilter.None)
                vm.intent(LibraryIntent.Initialize)
                advanceUntilIdle()
                val old = CompletableDeferred<List<LibraryDeck>>()
                repository.listAction = { old.await() }
                vm.intent(LibraryIntent.Retry)
                runCurrent()
                val newer = listOf(repository.preview.deck.copy(id = 2))
                repository.listAction = { newer }
                vm.intent(LibraryIntent.Retry)
                runCurrent()
                old.complete(listOf(repository.preview.deck))
                advanceUntilIdle()
                vm.state.value.decks
                    .toList() shouldBe newer
                repository.listAction = { error("offline") }
                vm.intent(LibraryIntent.Retry)
                advanceUntilIdle()
                vm.state.value.decks
                    .toList() shouldBe newer
                vm.state.value.failure shouldBe LibraryFailure.NETWORK
            }
        }
    })

private class FakeLibraryRepository : LibraryRepository {
    val preview = LibraryDetail(LibraryDeck(1, "토익 출제 단어 200개", "토익 준비를 위한 단어와 뜻", 2), emptyList())
    val personal = Deck(8, preview.deck.name, preview.deck.description, 2, 0, 0, DeckState.NOT_STARTED)
    val keys = mutableListOf<String>()
    var listAction: suspend () -> List<LibraryDeck> = { listOf(preview.deck) }
    var detailAction: suspend () -> LibraryDetail = { preview }
    var copyAction: suspend () -> Deck = { personal }

    override suspend fun list() = listAction()

    override suspend fun detail(libraryDeckId: Long) = detailAction()

    override suspend fun copy(
        libraryDeckId: Long,
        requestKey: String,
    ): Deck {
        keys.add(requestKey)
        return copyAction()
    }
}
