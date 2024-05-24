import React from "react";
import "styles/views/InfoWindow.scss";
import { Button } from "components/ui/Button";

interface InfoWindowProps {
  onClose: () => void; // Define the types for the props here
}

const InfoWindow: React.FC<InfoWindowProps> = ({ onClose }) => (
  <div className="info-window">
    <p className="info-title">Rules of the Game</p>
    <div className="info-content">
      <p>
        <strong>SwissQuiz</strong> is an interactive online quiz about the
        geography of Switzerland. In each round, you will see a random image
        from somewhere in Switzerland. Your task is to guess where the image was
        taken by clicking on a map of Switzerland.
      </p>
      <h2>🕹️ Game Parameters</h2>
      <p>
        The gamemaster, who initiates a game, can set a number of game
        parameters:
      </p>
      <ul>
        <li>
          <strong>Number of rounds</strong>: sets the number of rounds to be
          played in one game.
        </li>
        <li>
          <strong>Guessing time</strong>: sets the time interval for making a
          guess.
        </li>
        <li>
          <strong>Private game</strong>: will create a 6-digit PIN for the game
          such that only players with the PIN can join it. A game will stay
          private once this option has been selected.
        </li>
      </ul>
      <h2>🥇 Scoring</h2>
      <p>Points are awarded according to the following rules:</p>
      <ul>
        <li>For a perfect guess, you will be awarded 100 points.</li>
        <li>
          For every kilometer off from the correct location, one point will be
          deducted.
        </li>
        <li>
          No points will be awarded if your guess is 100 kilometers or more off.
        </li>
      </ul>
      <p>The player with the highest score at the end of the game wins!</p>
      <h2>⚡ Power-Ups</h2>
      <p>There are three power-ups, which you can use once in a game:</p>
      <ul>
        <li>
          <strong>Double Score</strong>: Doubles the points for the current
          round.
        </li>
        <li>
          <strong>Canton Hint</strong>: Highlights the boundaries of the canton
          where the image was taken.
        </li>
        <li>
          <strong>Triple Hint</strong>: Highlights the boundaries of three
          cantons, one of which is the canton where the image was taken.
        </li>
      </ul>
      <p>Good luck and have fun exploring Switzerland with SwissQuiz!</p>
    </div>
    <div className="button-container">
      <Button width="150px" onClick={onClose}>
        Close
      </Button>
    </div>
  </div>
);

export default InfoWindow;
