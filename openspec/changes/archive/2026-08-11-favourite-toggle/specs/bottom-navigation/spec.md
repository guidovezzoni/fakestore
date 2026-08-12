## MODIFIED Requirements

### Requirement: ProfileScreen shows a centred placeholder message
`:app` SHALL define a stateless `ProfileScreen(modifier: Modifier = Modifier)` composable displaying a single centred `Text` sourced from a `strings.xml` resource — "Profile coming soon…" — and no other content. `FavouritesScreen` is no longer a placeholder; its full behaviour (loading favourited products, empty state, favourite toggling, snackbar on failure) is defined by the `favourite-toggle` capability's `FavouritesScreen` requirement.

#### Scenario: ProfileScreen shows its placeholder message
- **WHEN** `ProfileScreen()` is composed
- **THEN** a centred text node showing the localised `profile_placeholder` string is displayed

### Requirement: Tab labels and the Profile placeholder string are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define `global_tab_products`, `global_tab_favourites`, `global_tab_profile`, and `profile_placeholder` as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`, and none SHALL appear as a hardcoded literal in `BottomNavigationBar.kt` or `ProfileScreen.kt`. The previously-required `favourites_placeholder` string is superseded by the `favourite-toggle` capability's real `FavouritesScreen` strings and MAY be removed once `FavouritesScreen` no longer references it.

#### Scenario: Every remaining string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** `global_tab_products`, `global_tab_favourites`, `global_tab_profile`, and `profile_placeholder` each exist in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: No hardcoded literals for the remaining placeholder string
- **WHEN** `BottomNavigationBar.kt` and `ProfileScreen.kt` are inspected
- **THEN** every tab label and the Profile placeholder message is sourced via `stringResource(R.string.*)`, with no inline string literals passed to `Text` or `label`
