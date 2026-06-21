🔧 Commands
Command	Permission	Description
/yaps	yaps.admin	Open the admin GUI (or yaps.gui)
/yaps reload	yaps.admin	Reload config + messages
/yaps reset <player>	yaps.admin	Reset player's VL
/yaps status <player>	yaps.admin	Show player VL/mute/ban
/yaps checks	yaps.admin	List all checks
/ban (-s) <name> <reason>	ab.ban	Permanent ban
/tempban (-s) <name> <time> <reason>	ab.tempban	Temporary ban
/ipban (-s) <name/IP> <reason>	ab.ipban	IP ban
/tempipban (-s) <name/IP> <time> <reason>	ab.tempipban	Temporary IP ban
/mute (-s) <name> <reason>	ab.mute	Permanent mute
/tempmute (-s) <name> <time> <reason>	ab.tempmute	Temporary mute
/kick (-s) <name> <reason>	ab.kick	Kick
/warn (-s) <name> <reason>	ab.warn	Warn
/tempwarn (-s) <name> <time> <reason>	ab.tempwarn	Temporary warn
/note <name> <reason>	ab.note	Add private note
/unban <name>	ab.unban	Unban
/unmute <name>	ab.unmute	Unmute
/unwarn <id>	ab.unwarn	Revoke warn by ID
/unnote <id>	ab.unnote	Revoke note by ID
/unpunish <id>	ab.unpunish	Delete any punishment by ID
/change-reason <id> <reason>	ab.change-reason	Edit punishment reason
/banlist [page]	ab.banlist	View active bans
/history <name> [page]	ab.history	Player punishment history
/warns [name] [page]	ab.warns.self / ab.warns.other	View warns
/notes <name> [page]	ab.notes	View notes
/check <name>	ab.check	Full player info
Time format: Xmo, Xd, Xh, Xm, Xs (e.g. 1mo2d3h, 30m, perma)

🔑 Permissions
Node	Default	Description
yaps.admin	op	Access /yaps admin commands
yaps.alerts	op	Receive anti-cheat alerts in chat
yaps.bypass	false	Bypass ALL anti-cheat checks
yaps.gui	op	Open the admin GUI
ab.all / ab.*	false	Wildcard — grant every ab.* node
ab.ban, ab.tempban, ab.ipban, ab.tempipban	op	Ban commands
ab.mute, ab.tempmute, ab.kick	op	Mute / kick commands
ab.warn, ab.tempwarn, ab.note	op	Warn / note commands
ab.unban, ab.unmute, ab.unwarn, ab.unnote, ab.unpunish	op	Revoke commands
ab.change-reason	op	Edit punishment reason
ab.banlist, ab.history, ab.notes, ab.check	op	Lookup commands
ab.warns.self	true	View own warns
ab.warns.other	op	View others' warns
ab.notify	op	See punishment broadcasts
ab.notify.silent	false	See silent (-s) punishment broadcasts
ab.undo.notify	op	See undo notifications
ab.tempban.dur.1 / .2 / .3 / .max	—	Duration cap tiers (600s / 3600s / 43200s / unlimited)
Same pattern applies for ab.tempwarn.dur.*, ab.tempmute.dur.*, ab.tempipban.dur.*.
