"""예매 전체 흐름 오케스트레이션 (Java TicketService 대응)."""

from utils.web_driver_factory import create_driver, quit_driver
from services.korail_login_service import login
from services.search_criteria_service import navigate_to_search, input_criteria
from services.ticket_reservation_service import search_and_reserve

_kept_sessions = []

async def reserve(korail_id, korail_pwd, departure, arrival, date, time_start, time_end, max_retries=20):
    chrome, driver = await create_driver()
    try:
        await login(driver, korail_id, korail_pwd)
        await navigate_to_search(driver)
        await input_criteria(driver, departure, arrival, date, time_start)
        ok = await search_and_reserve(driver, time_start, time_end, max_retries)

        if ok:
            url = await driver.current_url
            _kept_sessions.append(chrome)
            print("[완료] 예매 성공")
            return {"success": True, "message": "예매 성공! 열린 브라우저에서 결제를 완료하세요.", "url": url}

        await quit_driver(chrome)
        return {"success": False, "message": "조건에 맞는 예매 가능한 열차를 찾지 못했습니다.", "url": None}
    except Exception as e:
        await quit_driver(chrome)
        return {"success": False, "message": f"오류가 발생했습니다: {e}", "url": None}
