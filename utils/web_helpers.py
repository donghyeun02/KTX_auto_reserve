"""셀렉터/DOM 조작 공용 헬퍼 (selenium-driverless)."""

from selenium_driverless.types.by import By

LOGIN_URL = "https://www.korail.com/ticket/login"
SEARCH_URL = "https://www.korail.com/ticket/search/general"
SUCCESS_URL = "/ticket/reservation/detail"

ERROR_KEYWORDS = ["통신 중 오류", "오류", "매진", "실패", "예약하실 수 없", "불가", "잘못", "초과", "매크로"]

async def find_all(parent, css):
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

async def reload(driver):
    try:
        await driver.refresh()
    except Exception:
        try:
            url = await driver.current_url
            await driver.get(url)
        except Exception:
            pass

async def click_confirm(scope):
    """팝업의 확인/닫기 버튼을 셀렉터 → 텍스트 순으로 찾아 클릭한다."""
    for sel in [".btn_bn-blue.btn_pop-close", ".btn_by-blue.btn_pop-close",
                "button.btn_pop-close", ".btn_bn-blue", ".btn_by-blue"]:
        b = await first(scope, sel)
        if b:
            try:
                await b.click()
                return True
            except Exception:
                pass
    for b in await find_all(scope, "button, a"):
        if (await safe_text(b)) in ("확인", "예", "닫기"):
            try:
                await b.click()
                return True
            except Exception:
                pass
    return False

async def close_popups(driver):
    """공지(emer_pop) / 안내 / -8002 류 팝업 닫기 (예매 흐름과 무관)."""
    for pop in await find_all(driver, ".layerWrap.emer_pop"):
        await click_confirm(pop)
    for overlay in await find_all(driver, ".ReactModal__Overlay"):
        if await safe_text(overlay):
            await click_confirm(overlay)
