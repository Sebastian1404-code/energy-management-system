package distributedSystem.CustomerSupport.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class RuleBasedResponder {

    /**
     * Returns an auto-reply if any rule matches the user message.
     * If no rule matches, returns Optional.empty().
     */
    public Optional<String> match(String userMessageRaw) {
        String msg = normalize(userMessageRaw);

        // --- Rule 1: greeting ---
        if (containsAny(msg, List.of("hi", "hello", "hey", "good morning", "good afternoon", "good evening"))) {
            return Optional.of("Hi! 👋 How can I help you today?");
        }

        // --- Rule 2: help / capabilities ---
        if (containsAny(msg, List.of("help", "what can you do", "commands", "how to use", "support"))) {
            return Optional.of("I can help with: account/login, device setup, missing devices, consumption data, and alerts.");
        }

        // --- Rule 3: login/password issues ---
        if (containsAny(msg, List.of("login", "sign in", "can't log", "cannot log", "password", "reset password"))) {
            return Optional.of("If you can’t log in, try resetting your password from the login page. If it still fails, tell me what error you see.");
        }

        // --- Rule 4: register / create account ---
        if (containsAny(msg, List.of("register", "sign up", "create account", "new account"))) {
            return Optional.of("To create an account, go to Register, fill in your email and password, then follow the confirmation steps.");
        }

        // --- Rule 5: overconsumption / alerts ---
        if (containsAny(msg, List.of("overconsumption", "over consumption", "alert", "threshold", "limit", "notification"))) {
            return Optional.of("Overconsumption alerts trigger when a device exceeds its configured threshold for the monitored time window.");
        }

        // --- Rule 6: change threshold / configure alerts ---
        if (containsAny(msg, List.of("set threshold", "change threshold", "configure alert", "set limit", "change limit"))) {
            return Optional.of("You can configure alert thresholds from the Device Settings page. Select the device and update the threshold value.");
        }

        // --- Rule 7: device missing / offline ---
        if (containsAny(msg, List.of("device missing", "missing device", "device not found", "offline", "not showing", "disappeared"))) {
            return Optional.of("If a device is missing/offline, check it’s powered and connected, then refresh the dashboard. Also verify it is registered.");
        }

        // --- Rule 8: add/register device ---
        if (containsAny(msg, List.of("add device", "register device", "pair device", "enroll device"))) {
            return Optional.of("To add a device: Devices → Add Device, enter the device identifier, then save. It should appear in the device list.");
        }

        // --- Rule 9: consumption not updating / no data ---
        if (containsAny(msg, List.of("not updating", "no data", "stopped", "stale", "not refresh", "not refreshing"))) {
            return Optional.of("If consumption isn’t updating, refresh the page, check the device status, and ensure the simulator/monitoring pipeline is running.");
        }

        // --- Rule 10: billing/cost ---
        if (containsAny(msg, List.of("bill", "billing", "cost", "price", "tariff"))) {
            return Optional.of("This app tracks consumption and may show estimated cost if a tariff is configured in Settings.");
        }

        // --- Rule 11: thanks / goodbye ---
        if (containsAny(msg, List.of("thanks", "thank you", "thx", "bye", "goodbye"))) {
            return Optional.of("You’re welcome! If you need anything else, just message me anytime.");
        }

        // --- Rule 12: unclear / question marks ---
        if (msg.equals("?") || msg.equals("??") || msg.equals("???") || containsAny(msg, List.of("what", "why", "how"))) {
            return Optional.of("Can you share a bit more detail? For example: device name, what you expected, and what you currently see.");
        }

        return Optional.empty();
    }

    // ---------------- helpers ----------------

    private static boolean containsAny(String msg, List<String> needles) {
        for (String n : needles) {
            if (msg.contains(n)) return true;
        }
        return false;
    }

    /**
     * Normalize message:
     * - lower-case
     * - remove diacritics
     * - remove punctuation except spaces
     * - collapse whitespace
     */
    private static String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT).trim();

        String noDiacritics = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String noPunct = noDiacritics.replaceAll("[^a-z0-9\\s]", " ");

        return noPunct.replaceAll("\\s+", " ").trim();
    }
}
