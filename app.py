"""
코레일 KTX 자동 예매 API 진입점 (FastAPI + Swagger)

MVC 구조:
  controllers/ : API 라우터 (Controller)
  services/    : 로그인·검색·예매 비즈니스 로직 (Service)
  models/      : 요청/응답 스키마 (Model)
  utils/       : 드라이버 팩토리·DOM 헬퍼

실행:   uvicorn app:app --port 8080
Swagger: http://localhost:8080/docs
"""

from fastapi import FastAPI

from controllers.ticket_controller import router as ticket_router

app = FastAPI(
    title="코레일 KTX 자동 예매 API",
    description=(
        "출발역·도착역·날짜·시간 범위를 입력하면 selenium-driverless 로 "
        "로그인부터 예매까지 자동 수행합니다. 매크로 감지(-8002/-8004)를 우회합니다.\n\n"
        "**예매 성공 시 브라우저는 열린 채로 유지되니, 그 창에서 결제를 완료하세요.**"
    ),
    version="1.0.0",
)

app.include_router(ticket_router)

@app.get("/", include_in_schema=False)
async def root():
    return {"message": "KTX 자동 예매 API. Swagger UI 는 /docs 에서 확인하세요."}
