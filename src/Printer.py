def printer():
    print("Hello, World!")

def printer_with_message(message):
    print(message)

def printer_with_prefix(prefix, message):
    print(f"{prefix}: {message}")

def printer_with_suffix(message, suffix):
    print(f"{message} {suffix}")

def printer_with_format(message, format_type):
    if format_type == "uppercase":
        print(message.upper())
    elif format_type == "lowercase":
        print(message.lower())
    else:
        print(message)

def printer_with_repeat(message, times):
    for _ in range(times):
        print(message)

def printer_with_custom_format(message, format_function):
    formatted_message = format_function(message)
    print(formatted_message)    

def printer_with_condition(message, condition):
    if condition:
        print(message)
    else:
        print("Condition not met.")

def print100manually():
    for i in range(1, 101):
        print(i)

def print100withloop():
    for i in range(1, 101):
        print(i)    

def print100withlist():
    numbers = list(range(1, 101))
    for number in numbers:
        print(number)

def print100withwhile():
    i = 1
    while i <= 100:
        print(i)
        i += 1