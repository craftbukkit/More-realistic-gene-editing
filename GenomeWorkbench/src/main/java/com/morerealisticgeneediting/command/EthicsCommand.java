package com.morerealisticgeneediting.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.morerealisticgeneediting.ethics.EthicsCase;
import com.morerealisticgeneediting.ethics.EthicsCasebook;
import com.morerealisticgeneediting.screen.EthicsCaseScreenHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class EthicsCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("ethics")
                .requires(source -> source.hasPermissionLevel(2)) // Admin command
                .then(literal("present")
                        .then(argument("case_id", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String caseId = StringArgumentType.getString(context, "case_id");
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    EthicsCase ethicsCase = EthicsCasebook.getCase(caseId);

                                    if (player != null && ethicsCase != null) {
                                        player.openHandledScreen(
                                                new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
                                                    return new EthicsCaseScreenHandler(syncId, inv, ethicsCase);
                                                }, Text.of(ethicsCase.title()))
                                        );
                                        return 1; // Success
                                    } else {
                                        context.getSource().sendError(Text.of("Case not found or not a player."));
                                        return 0; // Failure
                                    }
                                })))
        );
    }
}
