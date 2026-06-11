"""API 모델 (Model 계층)."""

from typing import Optional

from pydantic import BaseModel

class ReserveResponse(BaseModel):
    success: bool
    message: str
    url: Optional[str] = None
