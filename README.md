**⚠️  &nbsp;해당 프로젝트는 개인적인 용도로만 사용하여 주시고, 불법적인 거래나 행위는 엄격히 금지됩니다.**  
**&nbsp; &nbsp; &nbsp; &nbsp;&nbsp;본 프로젝트 사용 시 관련 법률, 규정 및 서비스 약관을 준수하는 것은 사용자의 책임입니다.**  
**&nbsp; &nbsp; &nbsp; &nbsp;&nbsp;본 프로젝트의 사용으로 인한 어떠한 문제에 대해서도 본 프로젝트 개발자는 책임을 지지 않습니다.**

---

**⚠️  &nbsp;Please use this project for personal use only, and illegal transactions or acts are strictly prohibited.**  
**&nbsp; &nbsp; &nbsp; &nbsp;&nbsp;It is your responsibility to comply with the relevant laws, regulations and terms of service**

<br>
<br>

# KTX selenium-driverless 실증 프로토타입

Korail 매크로 감지(-8002/-8004 "통신 중 오류")를 **selenium-driverless**가 실제로 우회하는지 검증하는 최소 프로토타입.

- 일반 Selenium: CDP Runtime 누수로 **-8004** 차단
- 유저스크립트 `.click()`: `isTrusted=false`로 **통신 오류** 차단
- **selenium-driverless**: Runtime 누수 없이 조종 + CDP Input 클릭(isTrusted=true) → 두 장벽을 모두 건드림

## 설치

> ⚠️ selenium-driverless는 신생 라이브러리라 Python 3.14에선 호환이 안 될 수 있습니다.
> 안 되면 **Python 3.11 또는 3.12** 가상환경을 쓰세요.

```bash
cd KTX_auto_reserve

# (권장) 3.11/3.12 가상환경
python3.12 -m venv venv
source venv/bin/activate

pip install -r requirements.txt
```

Chrome 브라우저가 설치돼 있어야 합니다(드라이버는 자동).

## ⭐ 전체 자동 + Swagger (실사용) — `app.py`

로그인 → 검색조건 입력 → 예매까지 **전부 자동**. 값은 Swagger UI에서 입력.

```bash
cd KTX_auto_reserve
source venv/bin/activate          # 위에서 만든 3.12 venv
pip install -r requirements.txt   # fastapi, uvicorn 포함

uvicorn app:app --port 8080
```

브라우저에서 **http://localhost:8080/docs** 접속 →
`GET /ticket/reserve` 열기 → **Try it out** → 값 입력 → **Execute**:

| 파라미터   | 예시         | 설명                              |
| ---------- | ------------ | --------------------------------- |
| korailId   | 0123456789   | 코레일 회원번호                   |
| korailPwd  | **\*\*\*\*** | 비밀번호                          |
| departure  | 동대구       | 출발역('역' 제외)                 |
| arrival    | 서울         | 도착역                            |
| date       | 20260701     | 출발일 (yyyyMMdd)                 |
| timeStart  | 6            | 탐색 시작 시                      |
| timeEnd    | 10           | 탐색 종료 시                      |
| maxRetries | 20           | 자리 없을 때 새로고침 재시도 횟수 |

- 자리를 잡으면 응답에 `success: true` 가 오고, **열린 Chrome 창에서 직접 결제**하면 됩니다 (성공 시 브라우저를 닫지 않음).
- 자리가 없으면 `maxRetries` 만큼 새로고침하며 재시도 후 종료합니다.

> ⚠️ 예매 시도 동안 요청이 길게 걸립니다(브라우저가 떠서 동작). Swagger가 응답을 기다리는 게 정상입니다.

---

## 검증용 단발 스크립트 — `ktx_driverless_test.py`

검색조건은 직접 넣고 **예매 클릭만** 검증하는 최소 스크립트(처음 -8004 우회 확인용).

```bash
export KORAIL_ID="코레일아이디"
export KORAIL_PW="코레일비밀번호"
export KORAIL_START_HOUR=6     # 원하는 출발 시작 시
export KORAIL_END_HOUR=10      # 원하는 출발 끝 시

python3 ktx_driverless_test.py
```

### 흐름

1. 스크립트가 자동 로그인하고, 브라우저의 **지문(webdriver/cdc/chrome.runtime)**을 출력합니다.
2. 열린 브라우저에서 **직접** 출발/도착/날짜를 넣고 **조회**해 예매 목록(`/ticket/search/list`)을 띄웁니다.
3. 터미널에서 **Enter** → 스크립트가 시간대에 맞는 KTX 일반실을 찾아 **예매를 시도**합니다.
4. 결과:
   - `🎉 예매 성공` → driverless가 -8004를 우회함 (이 길로 본격 구현)
   - `❌ 여전히 통신 오류` → driverless로도 막힘 = Korail이 추가 지문(canvas/WebGL/TLS/행동 등)을 봄

## 주의

- 개인 학습/본인 예매 용도. 과도한 새로고침·요청은 자제하세요.
- 라이브러리/감지 업데이트로 언제든 깨질 수 있습니다(군비경쟁).
- API 호출에서 에러가 나면(예: `find_element` 시그니처 차이) 메시지를 공유해 주세요 — driverless 버전에 맞춰 조정합니다.
