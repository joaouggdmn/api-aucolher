package com.adocao.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Year;

/**
 * Ano de fundação da ONG, usado no cadastro e na edição do perfil.
 *
 * Opcional (nulo é válido), mas quando vem precisa estar entre {@value #MINIMO}
 * e o ano atual. O teto muda a cada ano, por isso não dá para usar @Min/@Max,
 * que só aceitam constantes.
 */
@Documented
@Constraint(validatedBy = AnoFundacao.Validador.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnoFundacao {

    int MINIMO = 1800;

    String message() default "Informe um ano de fundação entre 1800 e o ano atual";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<AnoFundacao, Integer> {

        @Override
        public boolean isValid(Integer ano, ConstraintValidatorContext context) {
            return ano == null || (ano >= MINIMO && ano <= Year.now().getValue());
        }
    }
}
