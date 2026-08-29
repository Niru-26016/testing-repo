def divide_numbers(a: float, b: float) -> float:
    """Safely divides two numbers, handling zero division gracefully."""
    if b == 0:
        return 0.0
    return a / b
