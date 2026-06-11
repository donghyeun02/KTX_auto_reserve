"""열차 탐색 및 예매 (Java TicketReservationService 대응)."""

import asyncio
import time

from selenium_driverless.types.by import By

from utils.web_helpers import (
    SUCCESS_URL, ERROR_KEYWORDS, find_all, first, safe_text, reload, close_popups, click_confirm,
)

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

async def matches(train, start_min, end_min):
    depart = await parse_depart_minutes(train)
    if depart is None or not (start_min <= depart <= end_min):
        return False
    if not await find_all(train, ".flag_wrap span.train_ktx_ticket"):
        return False
    if not await find_all(train, ".price_box.gen"):
        return False
    return True

async def wait_result(driver, timeout=40):
    deadline = time.time() + timeout
    info_popups = 0
    while time.time() < deadline:
        if SUCCESS_URL in (await driver.current_url):
            return "success"

        overlays = await find_all(driver, ".ReactModalPortal .ReactModal__Overlay")
        overlays += await find_all(driver, ".ReactModal__Overlay")
        overlays += await find_all(driver, ".layerWrap.emer_pop")
        for o in overlays:
            txt = await safe_text(o)
            if not txt:
                continue
            is_err = any(k in txt for k in ERROR_KEYWORDS)
            print("  [팝업] " + txt.replace("\n", " ")[:70])
            await click_confirm(o)
            if is_err:
                return "error"
            info_popups += 1
            if info_popups > 6:
                return "fail"
            await asyncio.sleep(0.7)
            break
        await asyncio.sleep(0.4)
    return "fail"

async def try_reserve_train(driver, train):
    boxes = await find_all(train, ".price_box.gen")
    if not boxes:
        return "fail"
    box = boxes[0]
    link = await first(box, "a") or box

    for attempt in range(1, 4):
        classes = await box.get_attribute("class") or ""
        if "active" not in classes:
            await link.click()
            await asyncio.sleep(1.2)

        reserve_btn = await first(driver, ".ticket_reserv_wrap .btn_bn-blue02.reservbtn")
        if reserve_btn is None:
            await asyncio.sleep(1)
            continue
        print(f"  [예매 시도 {attempt}/3] 예매 클릭")
        await reserve_btn.click()

        result = await wait_result(driver)
        if result == "success":
            return "success"
        if result == "error":
            await asyncio.sleep(4)
            continue
    return "fail"

async def search_and_reserve(driver, time_start, time_end, max_retries):
    start_min, end_min = time_start * 60, time_end * 60
    await close_popups(driver)
    await driver.find_element(By.CSS_SELECTOR, ".tckList.clear", timeout=30)

    for attempt in range(max_retries):
        trains = await find_all(driver, ".tckList.clear")
        print(f"[조회 {attempt + 1}/{max_retries}] 열차 {len(trains)}개")
        for train in trains:
            if await matches(train, start_min, end_min):
                if await try_reserve_train(driver, train) == "success":
                    return True
        await asyncio.sleep(3)
        await reload(driver)
        await close_popups(driver)
        try:
            await driver.find_element(By.CSS_SELECTOR, ".tckList.clear", timeout=20)
        except Exception:
            pass
    return False
