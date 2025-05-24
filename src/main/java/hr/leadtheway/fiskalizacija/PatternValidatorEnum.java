package hr.leadtheway.fiskalizacija;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PatternValidatorEnum implements ConstraintValidator<CustomPattern, Enum<?>> {
	@Override
	public boolean isValid(Enum<?> value, ConstraintValidatorContext constraintValidatorContext) {
		return true;
	}
}
