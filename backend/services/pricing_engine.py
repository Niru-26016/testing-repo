from typing import Dict, Any, List
from microservices.pricing_service import calculate_pricing_logic

def calculate_cart_summary(items: List[Dict[str, Any]], promo_code: str = "") -> Dict[str, Any]:
    """
    Pricing Engine module proxy delegating to Pricing Microservice domain logic.
    """
    return calculate_pricing_logic(items, promo_code)
