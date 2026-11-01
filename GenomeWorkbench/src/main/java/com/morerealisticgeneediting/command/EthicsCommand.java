package com.morerealisticgeneediting.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.morerealisticgeneediting.ethics.*;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;
public final class EthicsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(literal("ethics")
            .then(literal("list").executes(c->{EthicsCasebook.getAllCases().values().forEach(e->c.getSource().sendFeedback(()->Text.literal(e.id()+" — "+e.title()),false));return 1;}))
            .then(literal("read").then(argument("case",StringArgumentType.word())
                .suggests((c,b)->CommandSource.suggestMatching(EthicsCasebook.getAllCases().values().stream().map(EthicsCase::id),b))
                .executes(c->{var found=EthicsCasebook.getCase(StringArgumentType.getString(c,"case"));
                    if(found.isEmpty()){c.getSource().sendError(Text.literal("Unknown ethics case"));return 0;}
                    EthicsCase e=found.get();c.getSource().sendFeedback(()->Text.literal(e.title()+"\n"+e.description()),false);
                    e.options().forEach(o->c.getSource().sendFeedback(()->Text.literal(o.id()+": "+o.text()),false));return 1;})))
            .then(literal("choose").then(argument("case",StringArgumentType.word()).then(argument("option",StringArgumentType.word())
                .executes(c->{var found=EthicsCasebook.getCase(StringArgumentType.getString(c,"case"));
                    if(found.isEmpty())return 0;
                    String option=StringArgumentType.getString(c,"option");
                    var choice=found.get().options().stream().filter(o->o.id().equals(option)).findFirst();
                    if(choice.isEmpty()){c.getSource().sendError(Text.literal("Unknown choice"));return 0;}
                    c.getSource().sendFeedback(()->Text.literal(choice.get().consequence()),false);return 1;})))));
    }
}
