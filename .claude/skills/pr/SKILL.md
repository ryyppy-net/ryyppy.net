---
name: pr
description: Open a pull request for the current branch. Use whenever creating a PR or rewriting a PR description. A separate agent must review the diff and check that the description matches the changes before the PR is opened or updated.
---

# Opening a pull request

1. Base the PR on `main` (see CLAUDE.md). Commit and push the branch.
2. Draft the title and description from the full diff against `main`
   (`git diff origin/main...HEAD`), not from memory of the session.
   Keep the description concise:
   - Lead with why the change is made: the problem or need it addresses.
   - Then, briefly, what changes, kept high level. Leave line-by-line
     detail to the diff.
3. **Spawn a separate agent to review the description against the diff.**
   This step is mandatory; do not do the check yourself. Give the agent:
   - the base and head refs, so it reads the diff itself
     (`git diff origin/main...HEAD`, `git log origin/main..HEAD`);
   - the drafted title and description, verbatim;
   - no other context from this session: it judges only what the diff shows.

   Ask it to report:
   - claims in the description that the diff does not support;
   - changes in the diff the description does not mention
     (behaviour, schema migrations, config, dependencies, both front ends);
   - anything in the diff that looks unintended (debug code, stray files,
     unrelated edits);
   - whether the description leads with why, stays concise, and keeps
     technical detail high level.
4. Verify each issue the agent reports against the diff before acting on it:
   confirm it is a real problem, not a misreading. Drop the ones that are not,
   and note why. Fix the description for each confirmed mismatch. If a
   confirmed issue is an unintended change in the diff, fix the code instead,
   push, and repeat step 3 on the new diff.
5. Open the PR (or update its description) only once no confirmed issues
   remain. Repeat step 3 whenever the description is rewritten or new
   commits change what the PR does.
