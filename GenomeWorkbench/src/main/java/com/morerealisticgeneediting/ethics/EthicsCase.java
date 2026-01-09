package com.morerealisticgeneediting.ethics;

import net.minecraft.network.PacketByteBuf;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a single ethical case or dilemma presented to the player.
 * This is an immutable data class with network serialization capabilities.
 *
 * @param id          A unique identifier for this case (e.g., "case_crispr_off_target").
 * @param title       A short, display-friendly title for the case.
 * @param description A detailed description of the ethical scenario.
 * @param options     A list of choices the player can make in response to the dilemma.
 */
public record EthicsCase(
    String id,
    String title,
    String description,
    List<EthicsOption> options
) {

    /**
     * Writes the entire case to a network buffer for client-server communication.
     * @param buf The buffer to write to.
     */
    public void write(PacketByteBuf buf) {
        buf.writeString(id);
        buf.writeString(title);
        buf.writeString(description);
        buf.writeCollection(options, (b, option) -> option.write(b));
    }

    /**
     * Reads a case from a network buffer.
     * @param buf The buffer to read from.
     * @return A new EthicsCase instance.
     */
    public static EthicsCase from(PacketByteBuf buf) {
        String id = buf.readString();
        String title = buf.readString();
        String description = buf.readString();
        List<EthicsOption> options = buf.readList(EthicsOption::from);
        return new EthicsCase(id, title, description, options);
    }

    /**
     * Represents a single choice a player can make within an EthicsCase.
     * This is an immutable data class with network serialization capabilities.
     *
     * @param id          A unique identifier for this option (e.g., "option_proceed_cautiously").
     * @param text        The text displayed to the player for this choice.
     * @param consequence A description of the outcome or feedback for choosing this option.
     */
    public record EthicsOption(
        String id,
        String text,
        String consequence
    ) {
        /**
         * Writes the option to a network buffer.
         * @param buf The buffer to write to.
         */
        public void write(PacketByteBuf buf) {
            buf.writeString(id);
            buf.writeString(text);
            buf.writeString(consequence);
        }

        /**
         * Reads an option from a network buffer.
         * @param buf The buffer to read from.
         * @return A new EthicsOption instance.
         */
        public static EthicsOption from(PacketByteBuf buf) {
            String id = buf.readString();
            String text = buf.readString();
            String consequence = buf.readString();
            return new EthicsOption(id, text, consequence);
        }
    }
}
