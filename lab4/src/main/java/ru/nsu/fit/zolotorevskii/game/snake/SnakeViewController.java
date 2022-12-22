package ru.nsu.fit.zolotorevskii.game.snake;

import ru.nsu.fit.zolotorevskii.game.io.PlayerController;

public class SnakeViewController {
    private PlayerController playerController;

    public SnakeViewController(PlayerController playerController, SnakeView snakeView) {
        playerController.getNewMessageSubject().subscribe(messageWithSender -> {
            if (messageWithSender.getMessage().hasState()) {
                snakeView.setState(messageWithSender.getMessage().getState().getState());
            }
        });
    }
}
