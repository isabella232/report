package com.atlassian.performance.tools.report.api.action

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE_SUBMIT
import com.atlassian.performance.tools.jiraactions.api.observation.IssueObservation
import com.atlassian.performance.tools.report.api.result.EdibleResult
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Path

class EditedIssuesReport {

    fun report(
        results: List<EdibleResult>,
        output: Path
    ) {
        val cohorts = results.map { it.cohort }
        val headers = arrayOf("issue key") + cohorts
        val format = CSVFormat.DEFAULT.withHeader(*headers)
        val printer = CSVPrinter(output.toFile().ensureParentDirectory().bufferedWriter(), format)
        val editedIssues: List<EditedIssue> = listEditedIssues(results)
        editedIssues.forEach { issue ->
            val cells = listOf(issue.issueKey) + cohorts.map { issue.editCountPerCohort[it] }
            printer.printRecord(cells)
        }
        printer.flush()
    }

    private fun listEditedIssues(
        results: List<EdibleResult>
    ): List<EditedIssue> {
        val issueKeysPerCohort: Map<String, List<String>> = results
            .map { result ->
                val issueKeys = result
                    .actionMetrics
                    .filter { it.label == EDIT_ISSUE_SUBMIT.label }
                    .mapNotNull { it.observation }
                    .map { IssueObservation(it) }
                    .map { it.issueKey }
                return@map result.cohort to issueKeys
            }
            .toMap()
        val issueKeys = issueKeysPerCohort
            .values
            .asSequence()
            .flatten()
            .toSet()
        return issueKeys.map { issueKey ->
            EditedIssue(
                issueKey = issueKey,
                editCountPerCohort = results
                    .map { it.cohort }
                    .map mapCohort@{ cohort ->
                        val editCount = issueKeysPerCohort[cohort]
                            ?.filter { it == issueKey }
                            ?.size
                            ?: 0
                        return@mapCohort cohort to editCount
                    }
                    .toMap()
            )
        }
    }
}

private data class EditedIssue(
    val issueKey: String,
    val editCountPerCohort: Map<String, Int>
)