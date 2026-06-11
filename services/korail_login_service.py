"""코레일 로그인 (Java KorailLoginService 대응)."""

import asyncio

from selenium_driverless.types.by import By

from utils.web_helpers import LOGIN_URL, close_popups

async def login(driver, korail_id, korail_pwd):
    print("[로그인] 접속...")
    await driver.get(LOGIN_URL)
    await asyncio.sleep(2)
    await close_popups(driver)

    id_field = await driver.find_element(By.ID, "id", timeout=30)
    await id_field.send_keys(korail_id)
    pw_field = await driver.find_element(By.ID, "password", timeout=10)
    await pw_field.send_keys(korail_pwd)

    login_btn = await driver.find_element(By.CSS_SELECTOR, ".btn_bn-depblue", timeout=10)
    await login_btn.click()
    await asyncio.sleep(2.5)
    await close_popups(driver)
    print("[로그인] 완료")
