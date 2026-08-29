from typing import Dict, Any
from microservices.shipping_service import calculate_shipping_logic

def calculate_shipping_rates(address: Dict[str, Any], weight_kg: float = 1.0) -> Dict[str, Any]:
    """
    Shipping Engine module proxy delegating to Shipping Microservice domain logic.
    """
    return calculate_shipping_logic(address, weight_kg)
