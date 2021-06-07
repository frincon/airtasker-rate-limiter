import com.airtasker.ratelimiter.core.Rate;
import com.airtasker.ratelimiter.jaxrs.sample.CommonsCliParser;
import com.airtasker.ratelimiter.jaxrs.sample.InvalidCommandLineException;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class CommonsCliParserTest {

    private static final Rate DEFAULT_RATE = Rate.of(100, Duration.ofHours(1));

    @Test
    public void defaultValuesAreCorrect() throws InvalidCommandLineException {
        final var unit = new CommonsCliParser();
        final var options = unit.parseCommandLine(new String[] {});
        assertThat(options.getPort()).isEmpty();
        assertThat(options.getRate()).isEqualTo(DEFAULT_RATE);
    }

    @Test
    public void portShouldBeParserCorrectly() throws InvalidCommandLineException {
        final var port = RandomUtils.nextInt(0, 65535);
        final var unit = new CommonsCliParser();

        final var options = unit.parseCommandLine(new String[] {"-p", String.valueOf(port)});
        assertThat(options.getPort()).isEqualTo(Optional.of(port));
    }

    @Test
    public void shouldThrowExceptionWhenPortNotInRage() {
        final var unit = new CommonsCliParser();

        assertThatCode(() -> unit.parseCommandLine(new String[] {"-p", "-1"}))
                .isInstanceOf(InvalidCommandLineException.class);

        assertThatCode(() -> unit.parseCommandLine(new String[] {"-p", "65536"}))
                .isInstanceOf(InvalidCommandLineException.class);
    }

    @Test
    public void requestsShouldBeParserCorrectly() throws InvalidCommandLineException {
        final var requests = RandomUtils.nextLong(0, Long.MAX_VALUE);
        final var unit = new CommonsCliParser();

        final var options = unit.parseCommandLine(new String[] {"-r", String.valueOf(requests)});
        assertThat(options.getRate().requests()).isEqualTo(requests);
    }

    @Test
    public void shouldThrowExceptionWhenRequestsNegative() {
        final var unit = new CommonsCliParser();

        assertThatCode(() -> unit.parseCommandLine(new String[] {"-r", "-1"}))
                .isInstanceOf(InvalidCommandLineException.class);

    }

    @Test
    public void timeShouldBeParserCorrectly() throws InvalidCommandLineException {
        final var timeInMillis = RandomUtils.nextLong(1, Long.MAX_VALUE);
        final var unit = new CommonsCliParser();

        final var options = unit.parseCommandLine(new String[] {"-t", String.valueOf(timeInMillis)});
        assertThat(options.getRate().window()).isEqualTo(Duration.ofMillis(timeInMillis));
    }

    @Test
    public void shouldThrowExceptionWhenTimeIsNegativeOr0() {
        final var unit = new CommonsCliParser();

        assertThatCode(() -> unit.parseCommandLine(new String[] {"-t", "-1"}))
                .isInstanceOf(InvalidCommandLineException.class);

        assertThatCode(() -> unit.parseCommandLine(new String[] {"-t", "0"}))
                .isInstanceOf(InvalidCommandLineException.class);
    }

}
