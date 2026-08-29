from typing import Dict, Any, List

def calculate_cart_summary(items: List[Dict[str, Any]], promo_code: str = "") -> Dict[str, Any]:
    """
    Calculates subtotal, discount, tax, shipping, and total for e-commerce cart.
    BUG: Division by zero when promo code provides 100% discount (e.g. 'FREESHIP100').
    """
    subtotal = sum(item.get("price", 0.0) * item.get("quantity", 1) for item in items)
    
    VALID_PROMO_CODES = {
        "SAVE10": 10.0,
        "SAVE20": 20.0,
        "FREESHIP100": 100.0
    }
    
    discount_percent = 0.0
    if promo_code:
        code_upper = promo_code.strip().upper()
        # BUG LOCATION 2: Direct dict key lookup without fallback/get guard
        # Throws KeyError when invalid promo code is entered by user (e.g., 'INVALID50')
        discount_percent = VALID_PROMO_CODES[code_upper]
            
    discount_amount = subtotal * (discount_percent / 100.0)
    
    # Calculate tax scaling factor based on effective billable portion
    # BUG LOCATION: Lacks guard when discount_percent is 100.0, causing ZeroDivisionError
    effective_ratio = 1.0 - (discount_percent / 100.0)
    tax_factor = 0.08 / effective_ratio  # <--- ZeroDivisionError when discount_percent == 100
    
    tax_amount = round(subtotal * tax_factor * effective_ratio, 2)
    shipping_cost = 0.0 if (subtotal - discount_amount) > 50.0 else 9.99
    
    final_total = round((subtotal - discount_amount) + tax_amount + shipping_cost, 2)
    
    return {
        "subtotal": round(subtotal, 2),
        "discount_percent": discount_percent,
        "discount_amount": round(discount_amount, 2),
        "tax_amount": tax_amount,
        "shipping_cost": shipping_cost,
        "total": max(0.0, final_total)
    }
