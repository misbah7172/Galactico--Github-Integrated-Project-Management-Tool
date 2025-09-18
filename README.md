# Galactico--Github-Integrated-Project-Management-Tool
Galactico - GitHub Integrated Project & Team Management Tool Autotrack is a web-based application with a dedicated VS Code extension that helps development teams manage projects, track contributions, and streamline GitHub workflows. It simplifies task tracking, automates Git operations, and provides tools for team leads to review and approve contributions before merging.

Key Features Automated Git operations for consistent and simplified commits

Review-based contribution approval system for team leads

Real-time task status monitoring, including assigned tasks, deadlines, and completion status

VS Code extension for seamless project interaction and updates

GitHub webhook support for synchronized status and contribution tracking

Role-based access for contributors and team leads

Autotrack is designed to improve productivity, transparency, and collaboration in team-based software projects.

Branchs pull git bash command : git fetch --all && for br in $(git branch -r | grep -v '\->'); do git branch --track "${br#origin/}" "$br" 2>/dev/null; done

### Ai Review of my Branching style 
For a Solo/Small Team Project: Your current style is acceptable and shows good organization

For Enterprise/Team Project: Would benefit from more formal GitFlow implementation

Overall Rating: 6.5/10 - Good organization, needs process refinement

