from typing import Dict, Any

def calculate_shipping_rates(address: Dict[str, Any], weight_kg: float = 1.0) -> Dict[str, Any]:
    """
    Calculates estimated delivery days and shipping fee based on address details.
    BUG: Direct dictionary key access for 'zip_code' throws KeyError when optional field is missing.
    """
    country = address.get("country", "US").upper()
    
    if country == "US":
        # BUG LOCATION: Direct key lookup without dict.get() safety guard
        postal_code = address["zip_code"]  # <--- KeyError if zip_code missing in guest checkout
        prefix = postal_code[:3] if len(postal_code) >= 3 else "000"
        
        rate = 5.99 + (weight_kg * 1.5)
        if prefix.startswith("90"):
            rate += 2.50  # West Coast express surcharge
            
        return {
            "carrier": "USPS Priority Mail",
            "estimated_days": 2,
            "shipping_fee": round(rate, 2)
        }
    else:
        return {
            "carrier": "DHL Express International",
            "estimated_days": 5,
            "shipping_fee": round(19.99 + (weight_kg * 3.0), 2)
        }
