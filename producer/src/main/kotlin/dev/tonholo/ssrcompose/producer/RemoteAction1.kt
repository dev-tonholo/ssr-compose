package dev.tonholo.ssrcompose.producer

/**
 * A callback of one argument that runs on the server when the app triggers it.
 *
 * @param A the type of the argument the app sends.
 */
fun interface RemoteAction1<A> {
    operator fun invoke(argument: A)
}
