/**
 * Modern Jira-like Kanban board functionality for task management
 */
document.addEventListener('DOMContentLoaded', () => {
    // Initialize drag and drop functionality for kanban cards
    initializeDragAndDrop();
    
    // Initialize task status update functionality
    initializeTaskStatusUpdate();
    
    // Initialize quick actions
    initializeQuickActions();
    
    // Initialize card hover effects
    initializeCardEffects();
});

/**
 * Initialize drag and drop functionality for kanban cards
 */
function initializeDragAndDrop() {
    const draggables = document.querySelectorAll('.task-card');
    const dropzones = document.querySelectorAll('.task-list');
    
    // Add event listeners to draggable items
    draggables.forEach(draggable => {
        draggable.addEventListener('dragstart', (e) => {
            draggable.classList.add('dragging');
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/html', draggable.outerHTML);
        });
        
        draggable.addEventListener('dragend', () => {
            draggable.classList.remove('dragging');
            // Remove any drag-over states
            dropzones.forEach(zone => zone.classList.remove('drag-over'));
        });
    });
    
    // Add event listeners to dropzones
    dropzones.forEach(dropzone => {
        dropzone.addEventListener('dragover', e => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            dropzone.classList.add('drag-over');
            
            const dragging = document.querySelector('.dragging');
            if (dragging) {
                const afterElement = getDragAfterElement(dropzone, e.clientY);
                if (afterElement) {
                    dropzone.insertBefore(dragging, afterElement);
                } else {
                    dropzone.appendChild(dragging);
                }
            }
        });
        
        dropzone.addEventListener('dragleave', (e) => {
            // Only remove drag-over if we're leaving the dropzone entirely
            if (!dropzone.contains(e.relatedTarget)) {
                dropzone.classList.remove('drag-over');
            }
        });
        
        dropzone.addEventListener('drop', e => {
            e.preventDefault();
            dropzone.classList.remove('drag-over');
            
            const taskCard = document.querySelector('.dragging');
            const taskId = taskCard.getAttribute('data-task-id');
            const newStatus = dropzone.getAttribute('data-status');
            
            if (taskId && newStatus) {
                updateTaskStatus(taskId, newStatus);
            }
        });
    });
}

/**
 * Get the element to insert the dragged item after
 */
function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll('.task-card:not(.dragging)')];
    
    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        
        if (offset < 0 && offset > closest.offset) {
            return { offset: offset, element: child };
        } else {
            return closest;
        }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

/**
 * Initialize task status update functionality
 */
function initializeTaskStatusUpdate() {
    // Add event listeners to status buttons
    const statusButtons = document.querySelectorAll('.status-btn');
    statusButtons.forEach(button => {
        button.addEventListener('click', () => {
            const taskId = button.getAttribute('data-task-id');
            const newStatus = button.getAttribute('data-status');
            
            if (taskId && newStatus) {
                updateTaskStatus(taskId, newStatus);
            }
        });
    });
}

/**
 * Initialize quick actions functionality
 */
function initializeQuickActions() {
    // Toggle quick actions menu
    document.addEventListener('click', (e) => {
        if (e.target.matches('.card-action-btn')) {
            e.stopPropagation();
            const menu = e.target.closest('.card-actions').querySelector('.quick-actions-menu');
            
            // Close other open menus
            document.querySelectorAll('.quick-actions-menu').forEach(m => {
                if (m !== menu) m.style.display = 'none';
            });
            
            // Toggle current menu
            menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
        } else {
            // Close all menus when clicking outside
            document.querySelectorAll('.quick-actions-menu').forEach(m => {
                m.style.display = 'none';
            });
        }
    });
}

/**
 * Initialize card hover effects
 */
function initializeCardEffects() {
    const cards = document.querySelectorAll('.task-card');
    
    cards.forEach(card => {
        // Add visual feedback on hover
        card.addEventListener('mouseenter', () => {
            card.style.transform = 'translateY(-2px)';
        });
        
        card.addEventListener('mouseleave', () => {
            if (!card.classList.contains('dragging')) {
                card.style.transform = 'translateY(0)';
            }
        });
        
        // Add click to focus/select functionality
        card.addEventListener('click', (e) => {
            if (!e.target.matches('button, .quick-action')) {
                // Remove selection from other cards
                document.querySelectorAll('.task-card.selected').forEach(c => {
                    c.classList.remove('selected');
                });
                
                // Add selection to current card
                card.classList.add('selected');
            }
        });
    });
}

/**
 * Quick action functions
 */
function editTask(button) {
    const taskCard = button.closest('.task-card');
    const taskId = taskCard.getAttribute('data-task-id');
    
    // Navigate to task edit page
    window.location.href = `/tasks/${taskId}/edit`;
}

function moveTask(button, newStatus) {
    const taskCard = button.closest('.task-card');
    const taskId = taskCard.getAttribute('data-task-id');
    
    if (taskId && newStatus) {
        updateTaskStatus(taskId, newStatus);
    }
}

function deleteTask(button) {
    const taskCard = button.closest('.task-card');
    const taskId = taskCard.getAttribute('data-task-id');
    const taskTitle = taskCard.querySelector('.task-title').textContent;
    
    if (confirm(`Are you sure you want to delete "${taskTitle}"? This action cannot be undone.`)) {
        // Implement task deletion
        deleteTaskById(taskId);
    }
}

function deleteTaskById(taskId) {
    // Get CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    fetch(`/tasks/${taskId}`, {
        method: 'DELETE',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to delete task');
        }
        return response.json();
    })
    .then(data => {
        if (data.error) {
            showNotification(data.error, 'error');
        } else {
            showNotification('Task deleted successfully', 'success');
            
            // Remove the task card from DOM
            const taskCard = document.querySelector(`[data-task-id="${taskId}"]`);
            taskCard.remove();
            
            // Update task counts
            updateTaskCounts();
        }
    })
    .catch(error => {
        showNotification(error.message, 'error');
    });
}

/**
 * Update task status via AJAX
 */
function updateTaskStatus(taskId, newStatus) {
    const formData = new FormData();
    formData.append('status', newStatus);
    
    // Get CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    // Add loading state
    const taskCard = document.querySelector(`[data-task-id="${taskId}"]`);
    taskCard.classList.add('loading');
    
    fetch(`/tasks/${taskId}/status`, {
        method: 'POST',
        body: formData,
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to update task status');
        }
        return response.json();
    })
    .then(data => {
        if (data.error) {
            showNotification(data.error, 'error');
        } else {
            showNotification('Task status updated successfully', 'success');
            
            // Update task counts
            updateTaskCounts();
            
            // Update card visual state if needed
            if (newStatus === 'DONE') {
                taskCard.classList.add('completed');
            } else {
                taskCard.classList.remove('completed');
            }
        }
    })
    .catch(error => {
        showNotification(error.message, 'error');
    })
    .finally(() => {
        taskCard.classList.remove('loading');
    });
}

/**
 * Update task counts in column headers
 */
function updateTaskCounts() {
    const columns = document.querySelectorAll('.kanban-column');
    
    columns.forEach(column => {
        const taskList = column.querySelector('.task-list');
        const taskCount = taskList.querySelectorAll('.task-card').length;
        const countElement = column.querySelector('.task-count');
        
        if (countElement) {
            countElement.textContent = taskCount;
        }
        
        // Show/hide empty state
        const emptyState = taskList.querySelector('.empty-state');
        if (taskCount === 0 && !emptyState) {
            // Create empty state if it doesn't exist
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'empty-state';
            emptyDiv.innerHTML = `
                <div class="empty-illustration">
                    <i class="fas fa-clipboard-list"></i>
                </div>
                <p class="empty-message">No tasks in this column</p>
                <p class="empty-submessage">Drag tasks here or create new ones</p>
            `;
            taskList.appendChild(emptyDiv);
        } else if (taskCount > 0 && emptyState) {
            emptyState.remove();
        }
    });
}

/**
 * Show notification message with modern styling
 */
function showNotification(message, type) {
    // Remove existing notifications
    document.querySelectorAll('.notification').forEach(n => n.remove());
    
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${type === 'error' ? 'exclamation-circle' : 'check-circle'}"></i>
            <span>${message}</span>
            <button class="notification-close" onclick="this.parentElement.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    // Style the notification
    Object.assign(notification.style, {
        position: 'fixed',
        top: '20px',
        right: '20px',
        background: type === 'error' ? '#fee2e2' : '#d1fae5',
        color: type === 'error' ? '#dc2626' : '#059669',
        padding: '12px 16px',
        borderRadius: '8px',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
        zIndex: '9999',
        border: `1px solid ${type === 'error' ? '#fca5a5' : '#a7f3d0'}`,
        maxWidth: '400px'
    });
    
    document.body.appendChild(notification);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentElement) {
            notification.remove();
        }
    }, 5000);
    
    // Fade in animation
    notification.style.opacity = '0';
    notification.style.transform = 'translateX(100%)';
    setTimeout(() => {
        notification.style.transition = 'all 0.3s ease';
        notification.style.opacity = '1';
        notification.style.transform = 'translateX(0)';
    }, 10);
}
