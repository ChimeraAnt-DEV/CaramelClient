# The Xbox friends list (the card and modal that never showed a friend)

## Symptom

The Friends card on Home always drew its empty state, and the Friends modal listed the empty-state
sentence as a row. Neither ever showed a real Xbox Live friend.

## Cause

The whole read path already existed and was unit-tested — `XboxPeopleHub` parses
`/users/xuid({xuid})/people` and `XboxFriendsClient` fetches it with the account's own
`http://xboxlive.com` XSTS token (which the Bedrock chain already mints, so no extra consent). What
was missing was the wiring: nothing called `XboxFriendsClient.fetch`, `CaramelLauncherState` had no
friends field, and both screens hard-coded their empty states.

## Fix

- `CaramelLauncherState.friends` (list) and `friendsStatus` (a "loading" line, an error reason, or
  empty when loaded) carry the data.
- `MainActivity.loadFriends()` mints the XSTS token off the UI thread (`MsftAuthManager.xboxLiveXsts`)
  and fetches through `XboxFriendsClient`; `refreshState()` triggers it once per signed-in account id,
  so a recompose or a tab switch does not refetch. A missing token or a PeopleHub error becomes a
  status line, never an empty list with no explanation. `onDestroy` closes the client.
- `CaramelFriendRow` (the real picture, gamertag, presence) is drawn by the Home card (up to four)
  and by the modal's searchable `LazyColumn`. The modal filters on the gamertag and shows a distinct
  "no matching friends" empty state when a search excludes everything.

## Do not regress

- `FriendsListWiringTest` pins that the activity calls the client with the account's XSTS token and
  that both screens render `CaramelFriendRow`; it also pins that the modal no longer draws the
  empty-state body as a list item.
- The friends fetch must stay off the UI thread and must not be retriggered on every recompose.

## Requests (the "requests never show" bug)

Xbox Live keeps no friend-request object: a follow that is not returned **is** the request. The
launcher read only `/users/xuid({xuid})/people`, which is the accounts the caller follows, so:

- a request the caller **sent** was in the list but drew exactly like a friend (`Offline`), and
- a request the caller **received** was not in the list at all, because that person does not appear
  in the caller's own follows.

Fix, all unit-tested without a device:

- `XboxPeopleHub.parsePeople` keeps `isFollowingCaller` / `isFollowedByCaller`.
- `XboxPeopleHub.followersUrl` reads `/people/followers` (the accounts that follow the caller).
- `XboxPeopleHub.classify(following, followers, followersKnown)` splits the union into `friends`,
  `incoming`, `outgoing`. When the follow read failed it reports `followersKnown == false` and
  claims nothing: with per-entry flags it can still mark a sent request, and with no evidence at all
  the row stays a friend rather than inventing a request.
- `XboxFriendsClient.fetchGraph` does both reads on its own executor; `fetch` (single read) is kept
  for callers that only want the follow list.
- `CaramelFriendInfo.relation` (`CaramelFriendRelation`) is drawn by `CaramelFriendRow` as
  "Friend request received" / "Request sent" instead of a presence line, and `MainActivity` puts
  the received requests first so they are not below the fold.

Not verified on a device: that `/people/followers` is served for a Bedrock account's XSTS token.
The code degrades to the plain friend list with the error line when it is not, which is the old
behaviour, so a wrong path costs the request labels and nothing else.
