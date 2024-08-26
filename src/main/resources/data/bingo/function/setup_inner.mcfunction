$bingo start --gamemode lockout --allow-never-goals-in-lockout --size $(size) --difficulty $(difficulty) --require-client green pink
$execute if predicate bingo:$(consistent) run bingospreadplayers4d 0 0 500 @a[gamemode=!spectator] #bingo:excluded
$execute unless predicate bingo:$(consistent) in minecraft:overworld run bingospreadplayersseedfind 500 5 2500 $(respect_teams) @a[gamemode=!spectator] false #bingo:excluded
execute as @a[gamemode=!spectator] run clearspawnpoint
$execute at @p[gamemode=!spectator,team=green] run function bingo:init_team {team: "green", bonus_chest: "$(bonus_chest)", seed: $(seed), consistent: "$(consistent)"}
$execute at @p[gamemode=!spectator,team=pink] run function bingo:init_team {team: "pink", bonus_chest: "$(bonus_chest)", seed: $(seed), consistent: "$(consistent)"}
freezeplayers @a[gamemode=!spectator] 90
gamerule givePlayerTracker true
time set 0
weather rain 1
clear @a[gamemode=!spectator]
xp set @a[gamemode=!spectator] 0
xp set @a[gamemode=!spectator] 0 levels
effect clear @a[gamemode=!spectator]
gamemode survival @a[gamemode=!spectator]
advancement revoke @a[gamemode=!spectator] everything
difficulty hard
setentityval health @a[gamemode=!spectator] 20
setentityval food @a[gamemode=!spectator] 20
setentityval saturation @a[gamemode=!spectator] 5
setentityval exhaustion @a[gamemode=!spectator] 0
setstat @a[gamemode=!spectator] custom minecraft:time_since_rest 0
