Change Log
==========



Version 2.0.0 *(15 May 2019)*
----------------------------
Well making breaking changes just one day after official release is never good. Unless you have
a `A-ha` moment and a thing for breaking and remaking things. 

The second version of Elmo is a big step forward in writing a simpler code with less boilerplate.

Minor changes:

- `CommandUpdate` is renamed to `Update` and placed in it's own package `dev.boby.elmo.effect`
- `Update` is moved in it's own package`dev.boby.elmo.pure`

Major changes:

The `none` Command marker is removed from the old `CommandUpdate`.  The new `effect.Update`'s `update` method now has the following signature `fun update(msg: Message, model: Model): Return<Model, Command>`  which has two type constructors :
- Pure(model) which is a type alias for Pair(model,none) 
- Effect(model,cmd) which is a type alias for Pair(model,cmd)

So, where you previously had to write `Pair(model,update.none)` you can now write `Pure(model)` . The  benefit is that there is no more need to add one extra command and handle it as `Observable.empty()` . So this gives you less boilerplate and some minor performance improvements.



Version 1.0.0 *(14 May 2019)*
----------------------------

It is alive!

