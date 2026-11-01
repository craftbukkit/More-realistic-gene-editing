package com.morerealisticgeneediting.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.morerealisticgeneediting.project.*;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;
public final class ResearchCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(literal("research")
            .then(literal("list").executes(c->{
                var player=c.getSource().getPlayerOrThrow();
                ProjectRegistry.getAllProjects().forEach(p->c.getSource().sendFeedback(()->Text.literal(p.id()+" — "+p.title()+(p.isReferenceOnly()?" [reference]":"")),false));
                c.getSource().sendFeedback(()->Text.literal("Sample identity, edit revision and independent sequencing verification must match. Reference cases cannot unlock clinical achievements."),false);
                ServerProjectManager.acknowledgeIntroduction(player);return 1;}))
            .then(literal("read").then(argument("project",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(ProjectRegistry.getAllProjects().stream().map(ResearchProject::id),b)).executes(c->{
                var project=ProjectRegistry.getProject(StringArgumentType.getString(c,"project"));if(project==null)return 0;
                c.getSource().sendFeedback(()->Text.literal(project.title()+"\n"+project.description()),false);
                project.objectives().forEach(o->c.getSource().sendFeedback(()->Text.literal("• "+o),false));return 1;})))
            .then(literal("start").then(argument("project",StringArgumentType.word()).executes(c->{
                var player=c.getSource().getPlayerOrThrow();String id=StringArgumentType.getString(c,"project");ServerProjectManager.startProject(player,id);return ServerProjectManager.getPlayerState(player).isActive(id)?1:0;})))
            .then(literal("complete").then(argument("project",StringArgumentType.word()).executes(c->{
                var player=c.getSource().getPlayerOrThrow();String id=StringArgumentType.getString(c,"project");ServerProjectManager.completeProject(player,id);return ServerProjectManager.getPlayerState(player).isCompleted(id)?1:0;})))
            .then(literal("status").executes(c->{var player=c.getSource().getPlayerOrThrow();var state=ServerProjectManager.getPlayerState(player);
                c.getSource().sendFeedback(()->Text.literal("Active: "+state.activeProjectIds()+"\nCompleted: "+state.completedProjectIds()),false);
                var store=ProjectStatePersistence.getServerState(player.getServer());
                store.getEvidence(player.getUuid()).forEach(e->c.getSource().sendFeedback(()->Text.literal(e.sampleId()+" | sampled="+e.sampledRevision()+", edited="+e.editedRevision()+", verified="+e.verifiedRevision()),false));return 1;})));
    }
}
