"""
KTX 예매 자동화 실증 프로토타입 (selenium-driverless)

목적: selenium-driverless 가 Korail 의 매크로 감지(-8002/-8004, "통신 중 오류")를
      실제로 우회하는지 검증한다.

selenium-driverless 를 쓰는 이유:
  - CDP Runtime 도메인을 켜지 않고 Chrome 을 조종 → -8004(CDP 감지) 회피 시도
  - 클릭을 CDP Input 으로 보내 isTrusted=true → 프로그램 클릭 차단(통신 오류) 회피 시도
  - cdc_ 지문도 제거

흐름:
  1. 스크립트가 로그인
  2. 사용자가 브라우저에서 직접 출발/도착/날짜 조회 (검색 입력은 복잡해서 수동)
  3. 목록 페이지가 보이면 터미널에서 Enter
  4. 스크립트가 시간대에 맞는 KTX 일반실을 찾아 예매 클릭 → -8004 가 뜨는지 확인

실행:
  export KORAIL_ID=...   KORAIL_PW=...   KORAIL_START_HOUR=6   KORAIL_END_HOUR=10
  python3 ktx_driverless_test.py
"""

import asyncio
import os

from selenium_driverless import webdriver
from selenium_driverless.types.by import By

KORAIL_ID = os.environ.get("KORAIL_ID")
KORAIL_PW = os.environ.get("KORAIL_PW")
START_MIN = int(os.environ.get("KORAIL_START_HOUR", "6")) * 60
END_MIN = int(os.environ.get("KORAIL_END_HOUR", "10")) * 60

ERROR_KEYWORDS = ["통신 중 오류", "오류", "매진", "실패", "예약하실 수 없", "불가", "잘못", "초과", "매크로"]
SUCCESS_URL = "/ticket/reservation/detail"

async def find_all(parent, css):
    """존재 확인용: 없으면 빈 리스트."""
    try:
        return await parent.find_elements(By.CSS_SELECTOR, css)
    except Exception:
        return []

async def first(parent, css):
    els = await find_all(parent, css)
    return els[0] if els else None

async def safe_text(el):
    try:
        return (await el.text).strip()
    except Exception:
        return ""

async def close_popups(driver):
    """공지/매크로/안내 팝업을 닫는다(예매 흐름과 무관한 팝업)."""
    for btn in await find_all(driver, ".layerWrap.emer_pop .btn_by-blue.btn_pop-close"):
        try:
            await btn.click()
        except Exception:
            pass
    for btn in await find_all(driver, ".ReactModal__Overlay .btn_bn-blue.btn_pop-close, .ReactModal__Overlay .btn_by-blue.btn_pop-close"):
        try:
            await btn.click()
        except Exception:
            pass

async def print_fingerprint(driver):
    """driverless 가 정말 깨끗한 지문을 보이는지 확인(진단)."""
    js = (
        "return JSON.stringify({"
        "webdriver: navigator.webdriver,"
        "cdc: Object.getOwnPropertyNames(window).filter(p=>/cdc/i.test(p)),"
        "chromeRuntime: !!(window.chrome && window.chrome.runtime)"
        "})"
    )
    try:
        result = await driver.execute_script(js)
        print("[지문] " + str(result))
    except Exception as e:
        print("[지문] 확인 실패: " + str(e))

async def login(driver):
    print("[로그인] 로그인 페이지 접속...")
    await driver.get("https://www.korail.com/ticket/login")
    await asyncio.sleep(2)
    await close_popups(driver)

    id_field = await driver.find_element(By.ID, "id", timeout=30)
    await id_field.send_keys(KORAIL_ID)
    pw_field = await driver.find_element(By.ID, "password", timeout=10)
    await pw_field.send_keys(KORAIL_PW)
    print("[로그인] 아이디/비밀번호 입력 완료")

    login_btn = await driver.find_element(By.CSS_SELECTOR, ".btn_bn-depblue", timeout=10)
    await login_btn.click()
    await asyncio.sleep(2.5)
    await close_popups(driver)
    print("[로그인] 완료")

async def parse_depart_minutes(train):
    span = await first(train, ".data_box h3 span:last-child")
    if span is None:
        return None
    raw = (await safe_text(span)).replace("(", "").replace(")", "").replace(" ", "")
    part = raw.split("~")[0]
    if ":" not in part:
        return None
    try:
        hh, mm = part.split(":")
        return int(hh) * 60 + int(mm)
    except Exception:
        return None

async def try_reserve(driver):
    """목록에서 조건에 맞는 KTX 일반실을 찾아 예매 시도. 결과 문자열 반환."""
    trains = await find_all(driver, ".tckList.clear")
    if not trains:
        return "no_list"
    print(f"[조회] 열차 {len(trains)}개 발견")

    for i, train in enumerate(trains):
        depart = await parse_depart_minutes(train)
        ktx = bool(await find_all(train, ".flag_wrap span.train_ktx_ticket"))
        gen_boxes = await find_all(train, ".price_box.gen")
        gen = bool(gen_boxes)
        in_win = depart is not None and START_MIN <= depart <= END_MIN
        tstr = f"{depart // 60}:{depart % 60:02d}" if depart is not None else "?"
        print(f"[{i+1}번] 시각={tstr} KTX={ktx} 일반실={gen} 시간대={in_win}")

        if not (in_win and ktx and gen):
            continue

        box = gen_boxes[0]
        link = await first(box, "a") or box

        for attempt in range(1, 4):
            print(f"  [예매 시도 {attempt}/3] 일반실 선택 → 예매")
            classes = await box.get_attribute("class") or ""
            if "active" not in classes:
                await link.click()
                await asyncio.sleep(1.2)

            reserve_btn = await first(driver, ".ticket_reserv_wrap .btn_bn-blue02.reservbtn")
            if reserve_btn is None:
                print("  예매 버튼 못 찾음")
                await asyncio.sleep(1)
                continue
            await reserve_btn.click()

            result = await wait_result(driver)
            if result == "success":
                return "success"
            if result == "error":
                print("  → 통신 오류(매크로 감지) → 재시도")
                await asyncio.sleep(4)
                continue
    return "no_match_or_failed"

async def wait_result(driver, timeout=40):
    import time
    deadline = time.time() + timeout
    info_popups = 0
    while time.time() < deadline:
        url = await driver.current_url
        if SUCCESS_URL in url:
            return "success"
        overlays = await find_all(driver, ".ReactModalPortal .ReactModal__Overlay")
        for o in overlays:
            txt = await safe_text(o)
            if not txt:
                continue
            print("  [팝업] " + txt.replace("\n", " ")[:60])
            is_err = any(k in txt for k in ERROR_KEYWORDS)
            for sel in [".btn_bn-blue.btn_pop-close", ".btn_by-blue.btn_pop-close", "button.btn_pop-close"]:
                b = await first(o, sel)
                if b:
                    try:
                        await b.click()
                    except Exception:
                        pass
                    break
            if is_err:
                return "error"
            info_popups += 1
            if info_popups > 6:
                return "fail"
            await asyncio.sleep(0.7)
            break
        await asyncio.sleep(0.4)
    return "fail"

async def main():
    if not KORAIL_ID or not KORAIL_PW:
        print("환경변수 KORAIL_ID, KORAIL_PW 를 먼저 설정하세요.")
        return

    options = webdriver.ChromeOptions()
    async with webdriver.Chrome(options=options) as driver:
        await login(driver)
        await print_fingerprint(driver)

        print("\n" + "=" * 60)
        print("이제 열린 브라우저에서 직접:")
        print("  출발/도착/날짜 입력 → 조회 → 예매 목록(/ticket/search/list) 이 보이게 하세요.")
        print("준비되면 이 터미널에서 Enter 를 누르세요.")
        print("=" * 60)
        await asyncio.get_event_loop().run_in_executor(None, input)

        await close_popups(driver)
        result = await try_reserve(driver)
        print("\n[최종 결과] " + result)
        if result == "success":
            print("🎉 예매 성공! selenium-driverless 가 -8004 를 우회했습니다.")
        elif result == "error":
            print("❌ 여전히 통신 오류(-8004 류). driverless 로도 막힘 → 추가 지문 존재.")
        else:
            print("조건 맞는 열차가 없었거나 다른 문제. 로그를 확인하세요.")

        print("\n브라우저를 닫지 않습니다. 결과 확인 후 Enter 를 누르면 종료합니다.")
        await asyncio.get_event_loop().run_in_executor(None, input)

if __name__ == "__main__":
    asyncio.run(main())
