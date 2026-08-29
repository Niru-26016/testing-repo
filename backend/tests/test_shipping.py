import pytest
from services.shipping_engine import calculate_shipping_rates

def test_us_shipping_with_zip():
    address = {"country": "US", "zip_code": "90210", "city": "Beverly Hills"}
    result = calculate_shipping_rates(address, weight_kg=2.0)
    
    assert result["carrier"] == "USPS Priority Mail"
    assert result["shipping_fee"] > 0.0

def test_international_shipping():
    address = {"country": "DE", "city": "Berlin"}
    result = calculate_shipping_rates(address, weight_kg=1.0)
    
    assert result["carrier"] == "DHL Express International"
    assert result["estimated_days"] == 5
