from typing import Any, Dict, List


VALID_PROMO_CODES = {
    "SAVE10": 10.0,
    "SAVE20": 20.0,
    "FREESHIP100": 100.0,
}


def calculate_pricing_logic(
    items: List[Dict[str, Any]], promo_code: str = ""
) -> Dict[str, Any]:
    subtotal = sum(
        item.get("price", 0.0) * item.get("quantity", 1)
        for item in items
    )

    discount_percent = 0.0
    if promo_code:
        code_upper = promo_code.strip().upper()
        discount_percent = VALID_PROMO_CODES.get(code_upper, 0.0)

    discount_amount = subtotal * (discount_percent / 100.0)

    effective_ratio = 1.0 - (discount_percent / 100.0)
    if effective_ratio == 0.0:
        tax_factor = 0.0
    else:
        tax_factor = 0.08 / effective_ratio

    tax_amount = round(subtotal * tax_factor * effective_ratio, 2)
    shipping_cost = 0.0 if (subtotal - discount_amount) > 50.0 else 9.99
    final_total = round(
        (subtotal - discount_amount) + tax_amount + shipping_cost,
        2,
    )

    return {
        "subtotal": round(subtotal, 2),
        "discount_percent": discount_percent,
        "discount_amount": round(discount_amount, 2),
        "tax_amount": tax_amount,
        "shipping_cost": shipping_cost,
        "final_total": final_total,
    }
