# TODOS
## RIGHT NOW
- Publish db update
- Set debt to "Int?"? Cost can be null, but debt can't

### High Priority
- Traits need to apply to a card
- Many kingdoms are laggy
- Reload additional cards after vetoing

### Medium Priority
- "Banned" after banned cards
- Skeleton while loading!
- Sort popup instead of dropdown
- Add favorite star in grid view
- Black Market, Young witch (bane), Divine Wind, Ferryman, Riverboat, Way of the Mouse, Approaching Army
- Allow forcing a landscape type or expansion
- Landscapes: Allow / Exclude  / Force
- Show suggestions for types and categories while searching
- Set kingdom new to false after switching screens
- Add firebase logs for critical issues?
- Add missing card categories!!
- Allow more than 2 landscapes
- Add veto toast

### Low Priority
- Split icons for "both editions owned"
- When starting to edit a second name, stop editing the first one
- Delete toast queue
- Kingdom count
- Search for "can rip" shows cantrips
- Card function isBannable() / isFavoritable()
- Landscape cards color bar
- Check Cornucopia guilds -> When owning 2nd edition guilds and then cycling through cornucopia, guilds switches to NOT OWNED
- Swipe to fav / ban in options
- Reset button for fav / ban
- Color reset generation options red
- If only first edition is owned, always display first edition icon in card list -> komplexer, entweder muss jede Karte die ownership wissen, oder das kingdom muss es wissen

### Full features
- Deep links to kingdoms (kingdoms:// or **URL**)
- Build kingdom manually (FAB 2) + recommended sets / favorites tab
- Uploading / rating kingdoms
- VP Counter
- Translations
- Hint / popup system (i)

### Think about this
- Add split piles to additional material? (Upgrade cards are in there)
- Dominion Wiki link

### Code cleanup
- Use _somethingFromViewModel.update {} instead of setting _something.value
- Check logs for redundancy
- I think 'set' property can be removed from sets.json
- Applicationscope vs LifecycleScope vs CoroutineScope vs whatever
- Flows instead of lists from DAO?
- Try to thin out some parameters (TopBar)
- Add Modifier to Composable.parameters
- kotlin.collections.Set -> MutableSet / SET UMBENENNEN
- LinkedHashSets?
- Split Library and Kingdom card list class?

### Issues

### Might already be fixed
- Landscape veto
- Sauna and Avanto are generated distinctly
- There will be a problem in gridview when a card costs coins + debt. this is currently only the case for fortune and wedding, which are both not portrait supply cards.