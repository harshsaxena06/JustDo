package com.example.utils

import java.time.LocalDate

data class MotivationalQuote(
    val text: String,
    val author: String
)

object QuotesProvider {
    val quotes: List<MotivationalQuote> = listOf(
        MotivationalQuote("Small daily improvements over time lead to stunning results.", "Robin Sharma"),
        MotivationalQuote("Focus on being productive instead of busy.", "Tim Ferriss"),
        MotivationalQuote("The secret of getting ahead is getting started.", "Mark Twain"),
        MotivationalQuote("Action is the foundational key to all success.", "Pablo Picasso"),
        MotivationalQuote("You do not rise to the level of your goals. You fall to the level of your systems.", "James Clear"),
        MotivationalQuote("Amateurs sit and wait for inspiration, the rest of us just get up and go to work.", "Stephen King"),
        MotivationalQuote("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        MotivationalQuote("Do the hard jobs first. The easy jobs will take care of themselves.", "Dale Carnegie"),
        MotivationalQuote("Your future is created by what you do today, not tomorrow.", "Robert Kiyosaki"),
        MotivationalQuote("Lost time is never found again.", "Benjamin Franklin"),
        MotivationalQuote("Simplicity boils down to two steps: identify the essential, eliminate the rest.", "Leo Babauta"),
        MotivationalQuote("One thing at a time. Most important thing first. Start now.", "Caroline Webb"),
        MotivationalQuote("Progress, not perfection, builds unstoppable momentum.", "JustDo"),
        MotivationalQuote("Guard your focus like your most valuable asset—because it is.", "Cal Newport"),
        MotivationalQuote("Consistency beats intensity when building meaningful habits.", "JustDo"),
        MotivationalQuote("Plans are nothing; planning is everything.", "Dwight D. Eisenhower"),
        MotivationalQuote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        MotivationalQuote("It always seems impossible until it is done.", "Nelson Mandela"),
        MotivationalQuote("Well begun is half done.", "Aristotle"),
        MotivationalQuote("Every completed task is a promise kept to yourself.", "JustDo")
    )

    fun quoteForToday(offset: Int = 0): MotivationalQuote {
        val dayIndex = LocalDate.now().dayOfYear + offset
        val safeIndex = ((dayIndex % quotes.size) + quotes.size) % quotes.size
        return quotes[safeIndex]
    }
}
