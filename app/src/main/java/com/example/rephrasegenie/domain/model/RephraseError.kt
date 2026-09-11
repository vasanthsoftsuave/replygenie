package com.example.rephrasegenie.domain.model

/**
 * Every failure the user is allowed to see.
 *
 * [userMessage] is always already safe to show. Raw exception text, stack traces and the API key
 * must never reach the UI.
 */
sealed class RephraseError(val userMessage: String) : Exception(userMessage) {

    class Api(message: String) : RephraseError(message)

    class Validation(message: String) : RephraseError(message)

    /** The result failed the checks twice. Nothing is written back. */
    class Blocked(val reason: String) : RephraseError(
        "This rephrase was blocked (${friendlyReason(reason)}). " +
            "Try again or choose a different tone."
    )

    companion object {
        fun forHttpStatus(status: Int): Api = Api(
            when {
                status == 401 ->
                    "Unable to validate your OpenAI API key. Please check it and try again."

                status == 403 ->
                    "Your OpenAI API key does not have permission to do this. " +
                        "Please check your OpenAI account."

                status == 404 ->
                    "The requested OpenAI model is unavailable right now. Please try again later."

                status == 408 ->
                    "The request to OpenAI timed out. Please check your connection and try again."

                status == 429 ->
                    "OpenAI rate limit or quota exceeded. Please wait a moment, or check your " +
                        "OpenAI billing if this keeps happening."

                status >= 500 ->
                    "OpenAI is temporarily unavailable. Please try again shortly."

                else -> "Something went wrong talking to OpenAI. Please try again."
            }
        )

        fun timeout(): Api =
            Api("The request to OpenAI timed out. Please check your connection and try again.")

        fun offline(): Api =
            Api("Unable to reach OpenAI. Please check your internet connection and try again.")

        fun generic(): Api = Api("Something went wrong talking to OpenAI. Please try again.")

        fun emptyApiKey(): Validation = Validation("Please enter your OpenAI API key.")

        fun emptyUsername(): Validation = Validation("Please enter a username.")

        /**
         * Turns an internal reason into words a user can read.
         * "rewrite_conformance" becomes "it read as a reply instead of a reword".
         * "moderation:hate=92%,violence=61%" becomes "hate 92%, violence 61%".
         */
        private fun friendlyReason(reason: String): String = when {
            reason == "rewrite_conformance" -> "it read as a reply instead of a reword"

            reason.startsWith("moderation:") -> reason
                .removePrefix("moderation:")
                .split(",")
                .joinToString(", ") { it.replace("=", " ") }

            else -> reason
        }
    }
}
