# Rover SDK UI Layer Patterns

## View Models

-> must be a pure DAG

... question, is it OK to use a Subject to invert responsibility to keep
that DAG? It means we'd use calls into a view model instead of having
that view model subscribe to something in some cases.  Which is kind of
a weird switch of pattern.

## View Model Mixins

-> interface delegation approach

-> all ultimately must be merged into a single interface in a top-level
View Model that is meant to be bound directly to a view (and own that
view).  Class delegation is a good method of achieving this.

## Views

## View Mixins

## Observable Chains

-> events for both updating the UI and informing client view models of
actions

-> any side-effects must occur before a share() or shareAndReplay()
operator

-> new subscribers (particularly views) should immediately receive
enough events to fully populate them.  This adds some complexity,
however.  Particularly, the challenge of not duplicating side-effect
operations such as state updates or, worse, dispatching events (to say
nothing of duplicated computing). trade-offs with a "dispatch a refresh
action on subscribe" pattern? well, for starters, while it solves the
side-effect support issue above it introduces the issue of emitting
unnecessary updates to subscriber views.

Perhaps one solution to this problem is to implement the re-emitter as
the last stage of an observable chain (merge with a flatMap on the
shared epic that eats all the original events but re-emits on
subscribe), that has a whitelist of events to buffer and re-emit on
subscribe.  The prior steps are then shared.

-> should discuss the "refresh on subscribe" pattern, too.   So, I am
often doing a cached result.

## State Persistence
