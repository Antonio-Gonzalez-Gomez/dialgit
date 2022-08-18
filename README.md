# DialGit

DialGit is a work group management bot that relies on natural speech processing to perform its duties. It will link your Discord server to your Github repositories and Gitlab group to translate your planning into kanban boards and issues.

## How do I link it?

You must first install Dialgit in your Github repositories in this url: https://github.com/apps/dialgit. Then, you must create a Gitlab group and import those Github repositories in there. After all that, simply type "dg!start" to start a conversation with the bot. It will request the urls to your group and repositories and your Github username... and you will be good to go!

## How does it work?

After typing "dg!start", you can request one of several actions. Try to keep your petitions simple so DialGit can understand, for example "I want to add a Github issue". Here's a list of all things you can ask DialGit to do:

- Show this help message.
- Create an issue in a Github repository.
- Create a workspace in a Github repository.
- List all workspaces in a Github repository.
- List all Sprints in a Github workspace.
- Modify the Sprint configuration in a Github workspace.
- Perform a status report of your remaining work for an ongoing Github Sprint.
- Perform a group analysis of a Github Sprint.
- Perform an individual analysis of a Github Sprint.
- Create a board in a Gitlab repository.
- List all boards in a Gitlab repository.
- Create an issue in a Gitlab repository.

## How do I interact with the bot?

DialGit embeds use reaction emojis as buttons, so you can simply click on them. If you have doubts about what they do, click the ❓ button to see all buttons functionality. If you want to add information (like creating an issue), you can directly type the data field by field. You can also type the name of an element in a list of elements to select it. Additionally, when inputting a date, you can use almost any expression (like "tomorrow", "next Friday" or "the 20th of July") to do so.
