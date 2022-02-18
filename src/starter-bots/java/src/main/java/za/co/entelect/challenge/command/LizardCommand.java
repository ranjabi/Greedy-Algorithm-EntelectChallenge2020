package za.co.entelect.challenge.command;

public class LizardCommand implements Command {
    public LizardCommand() {
    }

    public String render() {
        return String.format("USE_LIZARD");
    }
}
