"""티켓 예매 API 라우터 (Controller 계층, Java TicketController 대응)."""

from fastapi import APIRouter, Query

from models.schemas import ReserveResponse
from services import ticket_service

router = APIRouter(tags=["Ticket"])

def _valid_date(date: str) -> bool:
    if not date or len(date) != 8 or not date.isdigit():
        return False
    try:
        month = int(date[4:6])
        day = int(date[6:8])
        return 1 <= month <= 12 and 1 <= day <= 31
    except ValueError:
        return False

@router.get("/ticket/reserve", summary="KTX 티켓 자동 예매", response_model=ReserveResponse)
async def reserve(
    korailId: str = Query(..., description="코레일 회원번호", examples=["0123456789"]),
    korailPwd: str = Query(..., description="코레일 비밀번호", examples=["password"]),
    departure: str = Query(..., description="출발역 ('역' 제외)", examples=["동대구"]),
    arrival: str = Query(..., description="도착역 ('역' 제외)", examples=["서울"]),
    date: str = Query(..., description="출발 날짜 (yyyyMMdd)", examples=["20260701"]),
    timeStart: int = Query(..., ge=0, le=23, description="탐색 시작 시간 (0~23)", examples=[6]),
    timeEnd: int = Query(..., ge=0, le=23, description="탐색 종료 시간 (0~23)", examples=[10]),
    maxRetries: int = Query(20, ge=1, le=200, description="자리 없을 때 새로고침 재시도 횟수", examples=[20]),
):
    """
    출발역, 도착역, 날짜, 시간 범위를 입력하여 코레일 KTX 티켓을 자동으로 예매합니다.

    - 시간대에 맞는 **KTX 일반실**을 찾아 예매를 시도하고, 없으면 새로고침하며 재시도합니다.
    - 성공하면 열린 브라우저에서 직접 **결제**를 완료하세요.
    """
    if not _valid_date(date):
        return ReserveResponse(success=False, message=f"날짜 형식이 올바르지 않습니다. yyyyMMdd 로 입력하세요: {date}")
    if timeStart >= timeEnd:
        return ReserveResponse(success=False, message="시작 시간은 종료 시간보다 앞서야 합니다.")

    result = await ticket_service.reserve(
        korailId, korailPwd, departure, arrival, date, timeStart, timeEnd, maxRetries
    )
    return ReserveResponse(**result)
