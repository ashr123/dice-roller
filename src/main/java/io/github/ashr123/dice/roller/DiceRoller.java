package io.github.ashr123.dice.roller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import picocli.CommandLine;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CommandLine.Command(name = "java -jar dice-roller.jar",
		showDefaultValues = true,
		mixinStandardHelpOptions = true,
		versionProvider = DiceRoller.class,
		description = "Dice roller for D&D.")
public class DiceRoller implements Runnable, CommandLine.IVersionProvider {
	private static final ThreadLocal<SecureRandom> SECURE_RANDOM_THREAD_LOCAL = ThreadLocal.withInitial(DiceRoller::getSecureRandom);
	@CommandLine.Parameters(description = "Is to show sum calculation? (values: true, false)",
			arity = "1",
			showDefaultValue = CommandLine.Help.Visibility.NEVER)
	private boolean isDetailed;
	@Positive
	@CommandLine.Parameters(description = "How many roles should the program make?",
			arity = "1",
			showDefaultValue = CommandLine.Help.Visibility.NEVER)
	private long roles;
	@Min(3)
	@CommandLine.Parameters(description = "Used to tell the number of side the dice has.",
			arity = "1",
			showDefaultValue = CommandLine.Help.Visibility.NEVER)
	private int d;
	@CommandLine.Parameters(description = "Used to be added per role.",
			index = "3",
			arity = "0..1")
	private int constantAddition;

	private DiceRoller() {
	}

	public static void main(String... args) {
		System.err.println("Starting...");
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		final CommandLine commandLine = new CommandLine(DiceRoller.class);
		final CommandLine.IExecutionStrategy executionStrategy = commandLine.getExecutionStrategy();
		commandLine.setExecutionStrategy(parseResult ->
				{
					if (!(parseResult.hasMatchedOption("name")
							|| parseResult.hasMatchedOption('h')
							|| parseResult.hasMatchedOption("version")
							|| parseResult.hasMatchedOption('V')))
						try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
							final Set<ConstraintViolation<Object>> violations = validatorFactory.getValidator().validate(parseResult.commandSpec().userObject());
							if (!violations.isEmpty()) {
								final StringBuilder errorMsg = new StringBuilder();
								for (ConstraintViolation<?> violation : violations)
									errorMsg.append("ERROR: ").append(violation.getPropertyPath()).append(' ').append(violation.getMessage()).append(System.lineSeparator());
								throw new CommandLine.ParameterException(parseResult.commandSpec().commandLine(), errorMsg.toString());
							}
						}
					return executionStrategy.execute(parseResult);
				})
				.execute(args);
	}

	private static SecureRandom getSecureRandom() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		System.err.println("Rolling...");
		d++;
		if (roles == 1 && (!isDetailed || constantAddition == 0)) {
			System.out.println(getSecureRandom().nextInt(1, d) + roles * constantAddition);
			return;
		}
		final IntStream intStream = IntStream.generate(() -> SECURE_RANDOM_THREAD_LOCAL.get().nextInt(1, d))
				.parallel().unordered()
				.limit(roles);
		if (isDetailed) {
			final int[] ints = intStream.toArray();
			System.err.print(IntStream.of(ints)
					.parallel().unordered()
					.mapToObj(String::valueOf)
					.collect(Collectors.joining(" + ")) + (constantAddition == 0 ? "" : " + " + roles + " × " + constantAddition) + " = ");
			System.out.println(IntStream.of(ints).parallel().unordered().sum() + roles * constantAddition);
		} else
			System.out.println(intStream.sum() + roles * constantAddition);
	}

	@Override
	public String[] getVersion() {
		return new String[]{"Dice roller v" + getClass().getPackage().getImplementationVersion()};
	}
}
