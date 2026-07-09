/**
 * Core of MiniGit - all structures, commands and the command line interface. Used with minigit command.
 */
module cz.cuni.mff.jordanpa.minigit {
    exports cz.cuni.mff.jordanpa.minigit;
    exports cz.cuni.mff.jordanpa.minigit.api;
    exports cz.cuni.mff.jordanpa.minigit.commands;
    exports cz.cuni.mff.jordanpa.minigit.misc;
    exports cz.cuni.mff.jordanpa.minigit.structures;

    uses cz.cuni.mff.jordanpa.minigit.commands.Command;

    provides cz.cuni.mff.jordanpa.minigit.commands.Command with
            cz.cuni.mff.jordanpa.minigit.commands.lowlevel.BlobCommand,
            cz.cuni.mff.jordanpa.minigit.commands.lowlevel.InspectCommand,
            cz.cuni.mff.jordanpa.minigit.commands.lowlevel.TreeCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.AddCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.AddIgnoredCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.AuthorCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.BlobDiffCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.BranchCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.BranchDeleteCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.CheckoutCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.CommitCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.HelpCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.InitCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.LogCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.MergeApplyCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.MergeCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.MergeStopCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.ProjectAddCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.ProjectRemoveCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.ProjectsCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.ProjectsInitCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.RefsCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.RestoreCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.RestoreStagedCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.ShowCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.StatusCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.TagCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.TagDeleteCommand,
            cz.cuni.mff.jordanpa.minigit.commands.highlevel.TreeCheckoutCommand;
}
