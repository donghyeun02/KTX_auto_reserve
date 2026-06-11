"""selenium-driverless Chrome 세션 생성/종료 (Java WebDriverFactory 대응)."""

from selenium_driverless import webdriver

async def create_driver():
    """Chrome 컨텍스트와 driver 를 생성해 (chrome, driver) 로 반환한다.

    selenium-driverless 는 CDP Runtime 누수 없이 조종하고, 클릭을 CDP Input 으로
    보내 isTrusted=true 로 인식시켜 Korail 매크로 감지(-8002/-8004)를 우회한다.
    """
    options = webdriver.ChromeOptions()
    chrome = webdriver.Chrome(options=options)
    driver = await chrome.__aenter__()
    return chrome, driver

async def quit_driver(chrome):
    try:
        await chrome.__aexit__(None, None, None)
    except Exception:
        pass
