from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

base=Path(__file__).resolve().parents[4]
out=base/'feature/learning/docs/assets'
out.mkdir(parents=True,exist_ok=True)
fonts=base/'core/designsystem/src/commonMain/composeResources/font'
blue='#4A6CF7'; ink='#101114'; muted='#444751'; pale='#EDF0FE'; bg='#F8FBFD'; border='#DCE2ED'

def font(size=18,bold=False):
    return ImageFont.truetype(str(fonts/('pretendard_bold.ttf' if bold else 'pretendard_regular.ttf')),size)
def text(x,y,s,size=18,color=ink,bold=False,max_width=None):
    if color==blue and size<19: color=muted
    f=font(size,bold)
    if max_width is not None:
        assert d.textlength(s,font=f)<=max_width,(s,d.textlength(s,font=f),max_width)
    d.text((x,y),s,font=f,fill=color)
def rect(x,y,w,h,fill='white',radius=16,outline=None):
    d.rounded_rectangle((x,y,x+w,y+h),radius,fill=fill,outline=outline,width=1)
def line(x,y,w): d.line((x,y,x+w,y),fill=border,width=1)
def phone(x,title,step):
    rect(x,166,392,968,'white',30,border)
    rect(x+1,167,390,122,blue,29)
    d.rectangle((x+1,223,x+391,287),fill=blue)
    text(x+24,184,'9:41',14,'white',True)
    text(x+24,228,'‹',28,'white')
    text(x+62,233,title,23,'white',True,290)
    text(x,122,step,19,blue,True,400)
    d.line((x+145,1114,x+247,1114),fill='#B5BAC4',width=4)
    return x+22

def scope(x,label='전체',editable=True,expanded=False):
    text(x,318,label,24,ink,True,254)
    if editable:
        icon_x=x+d.textlength(label,font=font(24,True))+8
        icon_y=324
        points=[(3,10),(7.667,5.333),(12.333,10)] if expanded else [(3,6),(7.667,10.667),(12.333,6)]
        d.line([(icon_x+px,icon_y+py) for px,py in points],fill='#444751',width=2,joint='curve')
def period_choices(x,y,active=0):
    for i,(s,w) in enumerate([('최근 7일',107),('최근 30일',113),('직접 설정',112)]):
        xx=x+[0,115,236][i]
        rect(xx,y,w,48,pale if i==active else bg,12)
        if i==active: d.line((xx+12,y+44,xx+w-12,y+44),fill=blue,width=2)
        text(xx+12,y+15,s,16,ink if i==active else muted,True,w-16)
def stat(x,y,values,labels):
    for i,(v,label) in enumerate(zip(values,labels)):
        xx=x+i*116
        text(xx,y,v,24,ink,True,112)
        text(xx,y+38,label,14,muted,False,112)
def chart(x,y,values):
    text(x,y,'날짜별 평가 횟수',18,ink,True)
    plot=y+44
    for frac,label in [(0,'30'),(.5,'15'),(1,'0')]:
        yy=plot+112*frac
        d.line((x+26,yy,x+345,yy),fill=border,width=1)
        text(x,yy-8,label,12,muted)
    for i,v in enumerate(values):
        xx=x+40+i*43
        h=v/30*112
        if h: rect(xx,plot+112-h,24,h,blue,5)
        text(xx-2,plot+124,str(10+i),13,muted)
    text(x+270,plot+148,'10월 · 학습일',12,muted)
def ratings(x,y,counts):
    text(x,y,'어떻게 평가했나요?',18,ink,True)
    for i,(label,n,col) in enumerate(zip(['쉬워요','애매해요','모르겠어요'],counts,[blue,'#000000','#FF7A70'])):
        yy=y+39+i*30
        d.ellipse((x,yy+3,x+10,yy+13),fill=col)
        text(x+20,yy,label,16,muted)
        text(x+291,yy,f'{n}회',16,ink,True)
def button(x,y,label,filled=False):
    rect(x,y,348,48,pale if filled else bg,12,blue if filled else border)
    text(x+16,y+14,label,17,ink,True,316)


def condition_sheet(x,selected='전체',many=False,error=False,custom=False):
    global d
    left=x-22
    region=im.crop((left+1,167,left+391,338)).convert('RGBA')
    mask=Image.new('L',region.size,0)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,390,968),29,fill=153)
    scrim=Image.new('RGBA',region.size,(0,0,0,0));scrim.putalpha(mask)
    im.paste(Image.alpha_composite(region,scrim).convert('RGB'),(left+1,167))
    d=ImageDraw.Draw(im)
    rect(left+1,338,390,793,'white',24,border)
    rect(x+125,351,100,4,'#C4C7CC',2)
    if custom:
        text(x,379,'기간 직접 설정',24,ink,True)
        text(x+302,384,'뒤로',16,muted,True)
    else:
        d.line([(x+2,385),(x+16,399)],fill=muted,width=2)
        d.line([(x+16,385),(x+2,399)],fill=muted,width=2)
        text(x+34,379,'조회 조건',24,ink,True)
        d.line([(x+318,391),(x+325,398),(x+339,382)],fill=blue,width=3)
    if custom:
        text(x,435,'시작일',16,muted)
        rect(x,465,348,52,bg,12,border);text(x+16,481,'2026.10.01',20,ink,True)
        text(x,540,'종료일',16,muted)
        rect(x,570,348,52,bg,12,border);text(x+16,586,'2026.10.16',20,ink,True)
        rect(x,650,348,260,bg,16)
        text(x+16,666,'2026년 10월',18,ink,True)
        for col,day in enumerate(['월','화','수','목','금','토','일']):
            text(x+23+col*46,706,day,14,muted)
        for day in range(1,32):
            pos=day+2;row=pos//7;col=pos%7
            xx=x+17+col*46;yy=737+row*30
            if day<=16: rect(xx-5,yy-5,36,28,pale,8,blue if day in (1,16) else None)
            text(xx,yy,str(day),14,ink if day<=16 else '#9DA3AF',day in (1,16))
        text(x,935,'시작일과 종료일을 모두 포함해요.',15,muted)
        text(x,963,'오늘 이후의 날짜는 선택할 수 없어요.',15,muted)
        line(x,1008,348);button(x,1032,'기간 선택 완료',True)
        return
    text(x,430,'기간',18,ink,True)
    period_choices(x,464)
    text(x,528,'2026.10.10 – 2026.10.16',15,muted)
    text(x,566,'덱',18,ink,True)
    rect(x,600,348,56,pale if selected=='전체' else bg,12)
    text(x+16,617,'전체',20,ink,True)
    if selected=='전체': d.line([(x+307,625),(x+313,631),(x+325,618)],fill=blue,width=3)
    line(x,669,348)
    # Only deck rows scroll; the period, All and header controls stay outside this viewport.
    layer=Image.new('RGB',(348,416),'white');d=ImageDraw.Draw(layer)
    if many:
        rows=[('영어 단어와 표현을 익히는','나만의 복습 덱'),('CS 핵심 개념',''),('일본어 기초',''),('여행 영어',''),('면접 준비',''),('한국사','')]
    else: rows=[('영어 단어',''),('CS 핵심 개념',''),('일본어 기초','')]
    yy=0
    for first,second in rows:
        h=80 if second else 56
        chosen=(first==selected) or (many and selected!='전체' and second!='')
        rect(0,yy,337,h,pale if chosen else 'white',12)
        text(16,yy+16,first,18,ink,True,285)
        if second: text(16,yy+45,second,18,ink,True,285)
        if chosen: d.line([(303,yy+28),(309,yy+34),(321,yy+21)],fill=blue,width=3)
        yy+=h+8
    if error:
        text(16,214,'덱을 더 불러오지 못했어요.',16,ink,True,315)
        rect(16,251,310,48,bg,12,border);text(32,266,'다시 시도',16,ink,True)
    im.paste(layer,(x,682));d=ImageDraw.Draw(im)


im=Image.new('RGB',(1800,1270),bg);d=ImageDraw.Draw(im)
text(48,30,'기간별 학습 리포트',34,ink,True)
text(48,82,'Step3 화면 구조 제안 · 앱 토큰 / Pretendard 사용 · 예시 수치 · 실제 앱 / Figma 원본 아님',19,muted)
a=phone(48,'학습 리포트','1. 전체 흐름과 누적 기록')
scope(a,'전체')
text(a,368,'2026.10.10 – 2026.10.16',15,muted)
rect(a,402,348,111,pale,16)
text(a+16,416,'전체 누적',15,blue,True)
text(a+16,447,'1,280회',24,ink,True);text(a+191,447,'42일',24,ink,True)
text(a+16,485,'누적 평가',13,muted);text(a+191,485,'평가한 학습일',13,muted)
text(a,537,'선택 기간',18,ink,True)
stat(a,572,['144회','96장','6일'],['평가 횟수','학습한 카드','평가한 학습일'])
assert sum([18,24,0,30,22,26,24])==144 and sum([82,38,24])==144
chart(a,665,[18,24,0,30,22,26,24])
ratings(a,877,[82,38,24])
button(a,1004,'학습 기록 보기')

b=phone(478,'학습 리포트','2. 덱 선택과 카드별 요약')
scope(b,'영어 단어')
text(b,368,'2026.10.10 – 2026.10.16',15,muted)
stat(b,402,['72회','36장','6일'],['평가 횟수','학습한 카드','평가한 학습일'])
assert sum([8,12,0,10,12,14,16])==72
chart(b,504,[8,12,0,10,12,14,16])
text(b,746,'카드별 평가',21,ink,True)
text(b,779,'기간 내 마지막 평가 기준',14,muted)
filter_x=b
for label,width,selected in [('전체',64,True),('쉬워요',76,False),('애매해요',84,False),('모르겠어요',100,False)]:
    rect(filter_x,804,width,48,pale if selected else bg,10)
    text(filter_x+12,820,label,14,ink if selected else muted,True,width-24)
    if selected: d.line((filter_x+12,848,filter_x+width-12,848),fill=blue,width=2)
    filter_x+=width+8
assert filter_x-8==b+348
for yy,name,rating,count in [(860,'abandon','애매해요','3회 · 10.16'),(945,'achieve','쉬워요','2회 · 10.16')]:
    text(b+4,yy,name,20,ink,True)
    text(b+4,yy+33,count,14,muted)
    text(b+236,yy+5,rating,15,blue,True,106)
    line(b,yy+70,348)

c=phone(908,'학습 기록','3. 날짜별 전체 기록 탐색')
scope(c,'전체')
text(c,368,'2026.10.10 – 2026.10.16',15,muted)
text(c,436,'2026년 10월 16일',19,ink,True)
for yy,name,n,detail in [(482,'영어 단어','16회','쉬워요 10 · 애매해요 4 · 모르겠어요 2'),(606,'CS 핵심 개념','8회','쉬워요 5 · 애매해요 2 · 모르겠어요 1')]:
    rect(c,yy,348,105,bg,14)
    text(c+16,yy+17,name,19,ink,True);text(c+278,yy+19,n,18,blue,True,60)
    text(c+16,yy+61,detail,14,muted,False,320)
text(c,759,'2026년 10월 15일',19,ink,True)
rect(c,801,348,105,bg,14)
text(c+16,818,'영어 단어',19,ink,True);text(c+278,820,'14회',18,blue,True)
text(c+16,862,'일일 학습 · 저장된 평가',14,muted)
button(c,947,'더 불러오기')
button(c,1007,'전체 기간 보기')
text(c,1074,'일일 학습에 저장한 평가만 표시해요.',14,muted)

q=phone(1338,'카드 평가 이력','4. 카드별 변화 확인')
scope(q,'영어 단어',editable=False)
rect(q,375,348,145,pale,16)
text(q+16,391,'현재 카드 내용',13,blue,True)
text(q+16,421,'abandon',24,ink,True)
text(q+16,467,'버리다, 포기하다',18,muted)
text(q,550,'최근 7일 · 평가 3회',18,ink,True)
text(q+236,552,'전체 기간',15,blue,True)
for yy,date,rating,col in [(607,'10.16  09:10','애매해요','#000000'),(729,'10.14  08:42','모르겠어요','#FF7A70'),(851,'10.12  19:05','쉬워요',blue)]:
    d.ellipse((q+1,yy+6,q+13,yy+18),fill=col)
    text(q+27,yy,date,17,muted)
    text(q+27,yy+37,rating,23,ink,True)
    line(q+27,yy+88,320)
text(q,1000,'학습일과 당시 기기 시간대 기준',14,muted)
text(q,1026,'평가는 내가 선택한 결과예요.',14,muted)
text(48,1175,'차트는 학습량, 평가 분포는 자기 평가. 정답률·암기 완료율로 표시하지 않습니다.',20,muted)
text(48,1211,'삭제 후에도 통계·평가 이력은 보존하고 삭제된 덱 이름·카드 본문은 숨깁니다(A안 확정).',18,muted)
im.save(out/'learning-report-flow.png')

im=Image.new('RGB',(1368,1240),bg);d=ImageDraw.Draw(im)
text(48,30,'기간 선택과 예외 상태',34,ink,True)
text(48,82,'Step3 화면 구조 제안 · 기능별 상태 확인용 · 실제 앱 screenshot 아님',19,muted)
a=phone(48,'학습 리포트','5. 시트 안에서 날짜 직접 설정')
scope(a,'전체',expanded=True)
text(a,368,'2026.10.10 – 2026.10.16',15,muted)
condition_sheet(a,custom=True)
b=phone(478,'학습 리포트','6. 기간 내 기록 없음')
scope(b,'전체')
text(b,368,'2026.10.10 – 2026.10.16',15,muted)
rect(b,450,348,98,pale,16)
text(b+16,466,'전체 누적',15,blue,True);text(b+16,500,'1,280회 · 42일',27,ink,True)
stat(b,591,['0회','0장','0일'],['평가 횟수','학습한 카드','평가한 학습일'])
text(b,720,'이 기간에 저장된',24,ink,True)
text(b,755,'학습 기록이 없어요.',24,ink,True)
text(b,808,'다른 기간의 기록을 확인해 보세요.',16,muted)
button(b,878,'전체 기간 보기',True)

c=phone(908,'학습 리포트','7. 조회 실패와 재시도')
scope(c,'전체')
text(c,368,'2026.10.10 – 2026.10.16',15,muted)
text(c,574,'기록을 불러오지 못했어요.',23,ink,True,348)
text(c,626,'연결 상태를 확인하고 다시 시도해 주세요.',15,muted,False,348)
button(c,707,'다시 시도',True)



text(48,1180,'로딩·휴식·삭제·추가 페이지 실패·인증 만료 상태의 동작은 설계 문서에 연결했습니다.',18,muted)
im.save(out/'learning-report-states.png')

im=Image.new('RGB',(1368,1240),bg);d=ImageDraw.Draw(im)
text(48,30,'덱 선택과 리포트 바로가기',34,ink,True)
text(48,82,'일반 진입은 전체 · 덱 바로가기는 해당 덱으로 진입 · 화면 구조 제안 / 예시 콘텐츠',19,muted)
a=phone(48,'학습 리포트','8. 덱과 기간을 함께 설정')
scope(a,'전체',expanded=True)
text(a,368,'2026.10.10 – 2026.10.16',15,muted)
condition_sheet(a)

# Panels 9-10 follow Figma PVw3ZFpRQf2JL3gv2AVOg9 [QA] Home 56795:3054 and Deck Progress 56795:2774.
# Only the '학습 리포트' controls are additions; the rest mirrors the existing screens.
def link_small(x,y,label):
    text(x,y,label,14,muted,True)
    ax=x+d.textlength(label,font=font(14,True))+6
    d.line([(ax,y+3),(ax+5,y+8),(ax,y+13)],fill=muted,width=2)
def badge(x,y,label,fill,color):
    w=d.textlength(label,font=font(12,True))+16
    rect(x-w,y,w,22,fill,11); text(x-w+8,y+4,label,12,color,True)

b=phone(478,'','9. 홈에서 전체 / 덱별 진입')
rect(b-21,167,390,220,blue,29); d.rectangle((b-21,223,b+369,387),fill=blue)
text(b,184,'9:41',14,'white',True)
text(b,222,'caro',26,'white',True)
text(b,268,'건형님, 안녕하세요!',24,'white',True,300)
text(b,306,'오늘은 복습 카드와 새 덱 하나를 끝내볼까요?',14,'white',False,330)
rect(b,338,142,30,'#3550C4',15); text(b+14,344,'6일 연속 학습 중',14,'white',True)
rect(b+150,338,118,30,'white',15); link_small(b+164,344,'학습 리포트')
d.rounded_rectangle((b+146,334,b+272,372),19,outline='#FFC857',width=2)
for yy,name,desc,badge_label,pct,btn in [(412,'영어 단어 1000개','일상에서 많이 쓰는 단어를 익혀요.','학습 중',.25,'이어서 학습'),(632,'CS 핵심 개념','면접 전에 꼭 보는 개념 모음','학습 전',0,'학습 시작')]:
    rect(b,yy,348,200,'white',20,'#C4C7CC')
    text(b+24,yy+22,name,18,ink,True,220)
    badge(b+324,yy+22,badge_label,pale,blue)
    text(b+24,yy+54,desc,14,muted,False,300)
    rect(b+24,yy+84,300,8,pale,4)
    if pct: rect(b+24,yy+84,300*pct,8,blue,4)
    text(b+24,yy+106,f'40개 카드 · {int(pct*100)}%',14,'#9DA3AF')
    rect(b+228,yy+100,96,32,pale,16); text(b+242,yy+107,btn,14,blue,True)
    d.line((b+24,yy+146,b+324,yy+146),fill=pale,width=1)
    link_small(b+24,yy+160,'학습 리포트')
    d.rounded_rectangle((b+18,yy+152,b+124,yy+188),10,outline='#FFC857',width=2)
text(b,862,'노란 테두리 = 기존 화면에 추가되는 부분',14,muted)
text(b,886,'헤더 칩: 전체 리포트 · 덱 하단 행: 해당 덱 리포트',14,muted)

c=phone(908,'영어 단어 1000개','10. 덱 상세에서 직접 진입')
d.line([(c+330,243),(c+330,243)],fill='white',width=4)
for dy in (0,8,16): d.ellipse((c+332,236+dy,c+336,240+dy),fill='white')
text(c,318,'하루 학습',20,ink,True)
badge(c+348,318,'학습 전',pale,blue)
text(c,352,'일상에서 많이 쓰는 단어를 예문 중심으로 익혀요.',14,muted,False,348)
rect(c,384,348,8,pale,4); rect(c,384,87,8,blue,4)
text(c,406,'40개 카드 · 25%',14,'#9DA3AF')
link_small(c+254,406,'학습 리포트')
d.rounded_rectangle((c+246,398,c+350,428),10,outline='#FFC857',width=2)
rect(c,446,348,50,blue,25); text(c+120,460,'일일학습 시작하기',18,'white',True)
rect(c,508,348,50,pale,25); text(c+112,522,'벼락치기 (전체학습)',18,blue,True)
d.line((c-22,580,c+370,580),fill=pale,width=6)
text(c,600,'전체 2,000',16,ink,True); text(c+88,601,'생성일순',14,muted)
d.line([(c+150,606),(c+155,611),(c+160,606)],fill=muted,width=2)
text(c+316,601,'편집',14,ink)
rect(c,636,348,50,blue,25); text(c+128,650,'+ 카드 추가하기',18,'white',True)
for yy,word,meaning,bl,bf,bc in [(704,'apple','사과','NEW','#FFF9EE','#FFC857'),(796,'run','달리다','REVIEW',pale,blue),(888,'water','물','HARD','#FFF2F1','#FF7A70')]:
    rect(c,yy,348,78,'white',16,'#C4C7CC')
    text(c+24,yy+16,word,18,ink,True); text(c+24,yy+44,meaning,15,ink)
    badge(c+324,yy+16,bl,bf,bc)
text(c,990,'노란 테두리 = 추가. 다른 요소는 Figma 기존 화면',14,muted)
text(48,1180,'전체 누적은 전체에서만 표시. 덱을 선택하면 누적 영역 없이 해당 기간의 통계와 카드별 평가를 보여줍니다.',18,muted)
im.save(out/'learning-report-selection.png')

# Focused deliverable: one selector design, closed and opened.
flow=Image.open(out/'learning-report-flow.png')
selection=Image.open(out/'learning-report-selection.png')
im=Image.new('RGB',(960,1190),bg);d=ImageDraw.Draw(im)
text(48,25,'학습 리포트 · 조회 조건',30,ink,True)
text(48,73,'리포트에는 덱 이름과 날짜만, 설정은 시트에서.',18,muted)
text(48,116,'기본 상태',16,blue,True)
text(520,116,'조회 조건을 연 상태',16,blue,True)
im.paste(flow.crop((48,166,440,1134)),(48,152))
im.paste(selection.crop((48,166,440,1134)),(520,152))
text(48,1146,'오른쪽 위 체크로 완료 · X로 취소하면 기존 조회 조건 유지',16,muted)
im.save(out/'learning-report-selector.png')
# Development review: long lists, pagination failure and narrow large-type layout.
im=Image.new('RGB',(1300,1230),bg);d=ImageDraw.Draw(im)
text(48,28,'개발 기준 · 덱 선택의 긴 목록과 작은 화면',30,ink,True)
text(48,78,'기간과 전체는 고정 · 덱 목록만 스크롤 · 상단 체크로 한 번에 반영',18,muted)
a=phone(48,'학습 리포트','11. 덱이 많은 조회 조건 시트')
scope(a,'전체',expanded=True)
condition_sheet(a,selected='영어 단어와 표현',many=True)

b=phone(478,'학습 리포트','12. 추가 목록 조회 실패')
scope(b,'영어 단어',expanded=True)
condition_sheet(b,selected='영어 단어',error=True)

# A 320pt reference with enlarged type: wrap the name and periods; stack summary values.
x=908;w=320
rect(x,166,w,968,'white',30,border);rect(x+1,167,w-2,122,blue,29)
d.rectangle((x+1,223,x+w-1,287),fill=blue)
text(x,122,'13. 320 폭 · 글자 확대 예시',18,blue,True)
text(x+24,184,'9:41',14,'white',True);text(x+24,233,'학습 리포트',25,'white',True)
xx=x+24;available=w-48
paragraph='영어 단어와 표현을 익히는 나만의 복습 덱'
words=paragraph.split();lines=[];current=''
for word in words:
    candidate=(current+' '+word).strip()
    if d.textlength(candidate,font=font(28,True))<=available-24: current=candidate
    else: lines.append(current);current=word
if current: lines.append(current)
y=312
for label in lines:
    text(xx,y,label,28,ink,True,available-24);y+=36
endx=xx+d.textlength(lines[-1],font=font(28,True))+8
d.line([(endx,y-23),(endx+5,y-18),(endx+10,y-23)],fill=muted,width=2)
y+=20
text(xx,y,'2026.10.10 – 2026.10.16',18,muted,False,available)
y+=52
for label,value in [('평가 횟수','72회'),('학습한 카드','36장'),('평가한 학습일','6일')]:
    rect(xx,y,available,68,bg,12)
    text(xx+14,y+25,label,18,muted,False,165)
    text(xx+200,y+19,value,25,ink,True,58)
    y+=80
text(xx,y+18,'날짜별 평가 횟수',21,ink,True)
for i,value in enumerate([8,12,0,10,12,14,16]):
    bx=xx+17+i*35;h=value*3.5
    rect(bx,y+135-h,20,max(1,h),blue,4)
    text(bx,y+148,str(i+10),14,muted)
d.line((x+110,1114,x+210,1114),fill='#B5BAC4',width=4)
text(48,1175,'레이아웃 예시입니다. 실제 스크롤·터치·스크린 리더·최대 시스템 글자 크기는 앱 구현 후 검증합니다.',17,muted)
im.save(out/'learning-report-development.png')

for p in out.glob('*.png'):
    with Image.open(p) as check:
        check.verify()
    print(p)
