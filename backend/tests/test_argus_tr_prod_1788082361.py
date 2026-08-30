from backend.microservices.pricing_service import calculate_pricing_logic


def test_argus_regression_unknown_save100_promo_code():
    result = calculate_pricing_logic(
        [{"price": 100.0, "quantity": 1}],
        promo_code="SAVE100",
    )

    assert result == {
        "subtotal": 100.0,
        "discount_percent": 0.0,
        "discount_amount": 0.0,
        "tax_amount": 8.0,
        "shipping_cost": 0.0,
        "total": 108.0,
    }
