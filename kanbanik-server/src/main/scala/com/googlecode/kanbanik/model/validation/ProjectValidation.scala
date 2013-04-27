package com.googlecode.kanbanik.model.validation
import com.googlecode.kanbanik.model.Project
import com.googlecode.kanbanik.model.Board
import com.googlecode.kanbanik.model.Task

trait ProjectValidation {

  def canBeDeleted(project: Project): (Boolean, String) = {
    val tasksOnProject = findTasksOnProject(project)
    if (tasksOnProject.isEmpty) {
      return (true, "")
    }

    return composeResult(tasksOnProject)
  }

  def canBeRemoved(project: Project, board: Board): (Boolean, String) = {
    val tasksOnProject = findTasksOnProject(project)
    if (tasksOnProject.isEmpty) {
      return (true, "")
    }

    val tasks = tasksOnProject.filter(_.workflowitem.parentWorkflow.board.id == board.id)
    if (tasks.size == 0) {
      return (true, "")
    }

    return composeResult(tasks)
  }

  private def composeResult(tasks: List[Task]): (Boolean, String) = {
      val header = "There are some tasks associated with this project. Please delete them first and than try to do this action again. The tasks: [";
      val body = tasks.map(task => task.ticketId + " on board: " + task.workflowitem.parentWorkflow.board.name)
      val footer = "]"
      
      val msg = header + body.mkString(",") + footer

      (false, msg)
  }

  // REALLY heavy operation! It is based on assumption that there will be only few boards 
  // in the system - mostly one. As soon as this will not be true anymore, needs to be optimized!
  private def findTasksOnProject(project: Project) = for (board <- Board.all(true); task <- board.tasks; if (task.project.equals(project))) yield task

}