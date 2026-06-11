"""예매 페이지 이동 + 검색 조건 입력 (Java SearchCriteriaService 대응)."""

import asyncio

from selenium_driverless.types.by import By

from utils.web_helpers import SEARCH_URL, find_all, first, safe_text, close_popups

async def navigate_to_search(driver):
    print("[이동] 예매 검색 페이지...")
    await driver.get(SEARCH_URL)
    await asyncio.sleep(1.5)
    await close_popups(driver)
    await driver.find_element(By.CSS_SELECTOR, ".btn_lookup", timeout=30)
    print("[이동] 검색 폼 준비 완료")

async def select_station(driver, station, is_departure):
    btn_sel = ".start .btn_pop" if is_departure else ".end .btn_pop"
    btn = await driver.find_element(By.CSS_SELECTOR, btn_sel, timeout=20)
    await btn.click()
    await asyncio.sleep(0.6)

    for link in await find_all(driver, ".list_wrap .ch_tag a"):
        if (await safe_text(link)) == station:
            await link.click()
            await asyncio.sleep(0.4)
            return
    raise RuntimeError(f"역 이름 '{station}'을(를) 리스트에서 찾지 못했습니다.")

async def set_hour(driver, start_hour):
    hour_str = f"{start_hour:02d}시"
    for scroll in range(12):
        hours = await find_all(driver, ".timeSelect li a")
        texts = [await safe_text(h) for h in hours]
        if scroll == 0:
            print(f"[시간] 후보 {len(hours)}개: {texts[:14]}")
        for h, t in zip(hours, texts):
            if (t == hour_str or t.replace(" ", "").startswith(f"{start_hour:02d}시")) \
                    and (await h.get_attribute("aria-disabled")) != "true":
                await h.click()
                print(f"[시간] '{t}' 선택")
                return True
        nexts = await find_all(driver, ".timeSelect .slick-next")
        if nexts:
            try:
                await nexts[0].click()
                await asyncio.sleep(0.3)
            except Exception:
                break
        else:
            break
    print(f"[시간] {hour_str} 못 찾음 — 위 후보 목록 확인 필요")
    return False

async def _dump_calendar(driver):
    try:
        html = await driver.execute_script(
            "var p=document.querySelector('.layerWrap, .calendar, .datepicker, table');"
            "return p ? p.outerHTML.slice(0,1200) : '(달력 컨테이너 못 찾음)';"
        )
        print("[달력 DOM] " + str(html).replace("\n", " "))
    except Exception as e:
        print("[달력 DOM] 덤프 실패: " + str(e))

async def set_date(driver, month, day, start_hour):
    btn = await driver.find_element(By.CSS_SELECTOR, ".btn_pop.btn_d-day", timeout=20)
    await btn.click()
    await asyncio.sleep(0.9)

    target_day = str(int(day))
    anchors = await find_all(driver, "table a")
    print(f"[날짜] 'table a' {len(anchors)}개 발견, 목표일={target_day}")

    clicked = False
    for a in anchors:
        day_span = await first(a, ".day")
        if day_span is None:
            continue
        text = await safe_text(day_span)
        disabled = await a.get_attribute("aria-disabled")
        if text == target_day and disabled != "true":
            await a.click()
            print(f"[날짜] {target_day}일 클릭 (table a > .day)")
            clicked = True
            break

    if not clicked:
        for d in await find_all(driver, "table .day"):
            if (await safe_text(d)) == target_day:
                await d.click()
                print(f"[날짜] {target_day}일 클릭 (.day 직접)")
                clicked = True
                break

    if not clicked:
        print(f"[날짜] {target_day}일 못 찾음 — 달력 구조 확인 필요")
        await _dump_calendar(driver)

    await set_hour(driver, start_hour)

    confirm = await driver.find_element(By.CSS_SELECTOR, ".btn_wrap .btn_bn-blue", timeout=10)
    await confirm.click()
    await asyncio.sleep(0.5)

async def input_criteria(driver, departure, arrival, date, time_start):
    print(f"[입력] {departure} → {arrival}, {date}, {time_start}시~")
    await select_station(driver, departure, True)
    await select_station(driver, arrival, False)
    await set_date(driver, date[4:6], date[6:8], time_start)
    lookup = await driver.find_element(By.CSS_SELECTOR, ".btn_lookup", timeout=20)
    await lookup.click()
    print("[입력] 조회 클릭 완료")
