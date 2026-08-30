from backend.microservices.pricing_service import calculate_pricing_logic


def test_argus_regression_unknown_promo_code_dsf():
    result = calculate_pricing_logic(
        items=[{"price": 25.0, "quantity": 4}],
        promo_code="DSF",
    )

    assert result == {
        "subtotal": 100.0,
        "discount_percent": 0.0,
        "discount_amount": 0.0,
        "tax_amount": 8.0,
        "shipping_cost": 0.0,
        "total": 108.0,
    }
